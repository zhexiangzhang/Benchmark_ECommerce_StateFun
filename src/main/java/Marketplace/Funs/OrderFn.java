package Marketplace.Funs;

import Common.Entity.*;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToCartFn.CheckoutCartResult;
import Marketplace.Types.MsgToCustomer.NotifyCustomer;
import Marketplace.Types.MsgToOrderFn.OrderStateUpdate;
import Marketplace.Types.MsgToPaymentFn.ProcessPayment;
import Marketplace.Types.MsgToStock.CheckoutResv;
import Marketplace.Types.State.OrderAsyncTaskState;
import Marketplace.Types.State.OrderState;
import Marketplace.Types.State.OrderTempInfoState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class OrderFn implements StatefulFunction {
    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "order");

    Logger logger = Logger.getLogger("OrderFn");

    // generate unique Identifier
    static final ValueSpec<Long> ORDERIDSTATE = ValueSpec.named("orderId").withLongType();
    static final ValueSpec<Long> ORDERHISTORYIDSTATE = ValueSpec.named("orderHistoryId").withLongType();
    // store checkout info
    static final ValueSpec<OrderTempInfoState> TEMPCKINFOSTATE = ValueSpec.named("tempCKInfoState").withCustomType(OrderTempInfoState.TYPE);
    // tmp store async task state
    static final ValueSpec<OrderAsyncTaskState> ASYNCTASKSTATE = ValueSpec.named("asyncTaskState").withCustomType(OrderAsyncTaskState.TYPE);

    // store order info
    static final ValueSpec<OrderState> ORDERSTATE = ValueSpec.named("orderState").withCustomType(OrderState.TYPE);

    //  包含了创建函数实例所需的所有信息
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpecs(ORDERIDSTATE, ASYNCTASKSTATE, TEMPCKINFOSTATE, ORDERSTATE, ORDERHISTORYIDSTATE)
            .withSupplier(OrderFn::new)
            .build();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            // cart --> order, (checkout request)
            if (message.is(Checkout.TYPE)) {
                onAtptReservation(context, message);
            }
            // stock --> order, (checkout response)
            else if (message.is(CheckoutResv.TYPE)) {
                onHandleCheckoutResponse(context, message);
            }
            // xxxx ---> order (update order status)
            else if (message.is(OrderStateUpdate.TYPE)) {
                OrderStateUpdate info = message.as(OrderStateUpdate.TYPE);
                UpdateOrderStatus(context, info.getOrderId(), info.getOrderStatus());
            }
        } catch (Exception e) {
            System.out.println("OrderFn error: !!!!!!!!!!!!" + e.getMessage());
            e.printStackTrace();
        }
        return context.done();
    }

//    ===============================================================================
//                                  helper functions
//    ===============================================================================

    private OrderAsyncTaskState getAtptResvTaskState(Context context) {
        return context.storage().get(ASYNCTASKSTATE).orElse(new OrderAsyncTaskState());
    }

    private OrderTempInfoState getTempCKInfoState(Context context) {
        return context.storage().get(TEMPCKINFOSTATE).orElse(new OrderTempInfoState());
    }

    private OrderState getOrderState(Context context) {
        return context.storage().get(ORDERSTATE).orElse(new OrderState());
    }

    private Long generateNextOrderID(Context context) {
        Long nextId = context.storage().get(ORDERIDSTATE).orElse(0L) + 1;
        context.storage().set(ORDERIDSTATE, nextId);
        // different partitionId may have same orderId, so we add partitionId number at beginning
        return nextId;
    }

    private Long generateNextOrderHistoryID(Context context) {
        Long nextId = context.storage().get(ORDERHISTORYIDSTATE).orElse(0L) + 1;
        context.storage().set(ORDERHISTORYIDSTATE, nextId);
        return nextId;
    }

    private String getPartionText(String id) {
        return String.format("[ OrderFn partitionId %s ] ", id);
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private void showLogPrt(String log) {
//        logger.info(log);
        System.out.println(log);
    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }

//    ====================================================================================
//    Attemp/Confirm/Cance  Reservation (two steps business logic)【send message to stock】
//    ====================================================================================

    private void onAtptReservation(Context context, Message message) {
        OrderAsyncTaskState atptResvTaskState = getAtptResvTaskState(context);
        OrderTempInfoState tempCKInfoState = getTempCKInfoState(context);
        Checkout checkout = message.as(Checkout.TYPE);

        Map<Long, BasketItem> items = checkout.getItems();
        long customerId = checkout.getCustomerCheckout().getCustomerId();
        int nItems = items.size();

        tempCKInfoState.addCheckout(customerId, checkout);
//        atptResvTaskState.addNewTaskCnt(customerId, nItems);
        atptResvTaskState.addNewTask(customerId, nItems, Enums.TaskType.AttemptReservationsType);
        context.storage().set(ASYNCTASKSTATE, atptResvTaskState);
        context.storage().set(TEMPCKINFOSTATE, tempCKInfoState);

        for (Map.Entry<Long, BasketItem> entry : items.entrySet()) {
            int stockPartitionId = (int) (entry.getValue().getProductId() % Constants.nStockPartitions);
            sendMessage(context,
                    StockFn.TYPE,
                    String.valueOf(stockPartitionId),
                    CheckoutResv.TYPE,
                    new CheckoutResv(
                            customerId,
                            entry.getValue(),
                            Enums.TaskType.AttemptReservationsType,
                            Enums.ItemStatus.UNKNOWN));
        }

        String log = getPartionText(context.self().id())
                + "OrderFn: attempt reservation, message sent to stock, customerId: "
                + customerId + "\n";
        showLogPrt(log);
    }

    //    confirm reservation or cancel reservation (second step, send confirm or cancel message to stock)
    private void onDecideReservations(Context context, List<CheckoutResv> checkoutResvs, Enums.TaskType taskType) {
        for (CheckoutResv checkoutResv : checkoutResvs) {
            int stockPartitionId = (int) (checkoutResv.getItem().getProductId() % Constants.nStockPartitions);
            checkoutResv.setTaskType(taskType);
            sendMessage(context,
                    StockFn.TYPE,
                    String.valueOf(stockPartitionId),
                    CheckoutResv.TYPE,
                    checkoutResv);
        }
        String log = getPartionText(context.self().id())
                + "OrderFn: decide reservation, message sent to stock, customerId: " + taskType + "\n";
        showLogPrt(log);
    }

//    ====================================================================================
//                  handle checkout response 【receive message from stock】
//    ====================================================================================

    private void onHandleCheckoutResponse(Context context, Message message) {
        CheckoutResv checkoutResv = message.as(CheckoutResv.TYPE);
        Enums.TaskType taskType = checkoutResv.getTaskType();
        long customerId = checkoutResv.getCustomerId();
        switch (taskType) {
            case AttemptReservationsType:
                String log = getPartionText(context.self().id())
                        + " #sub-task#, attempt reservation response, customerId: " + customerId ;
                showLogPrt(log);
                dealAttemptResponse(context, checkoutResv);
                break;
            case ConfirmReservationsType:
                String log2 = getPartionText(context.self().id())
                        + " #sub-task#, confirm reservation response, customerId: " + customerId ;
                showLogPrt(log2);
                dealConfirmResponse(context, customerId);
                break;
            case CancelReservationsType:
                String log3 = getPartionText(context.self().id())
                        + " #sub-task#, cancel reservation response, customerId: " + customerId ;
                showLogPrt(log3);
                dealCancelResponse(context, customerId);
                break;
            default:
                break;
        }
    }

    private void dealAttemptResponse(Context context, CheckoutResv checkoutResv) {
        long customerId = checkoutResv.getCustomerId();
        OrderAsyncTaskState atptResvTaskState = getAtptResvTaskState(context);
        atptResvTaskState.addCompletedSubTask(customerId, checkoutResv, Enums.TaskType.AttemptReservationsType);
        boolean isTaskComplete = atptResvTaskState.isTaskComplete(customerId, Enums.TaskType.AttemptReservationsType);
        if (isTaskComplete) {
            List<CheckoutResv> checkoutResvs = atptResvTaskState.getSingleCheckoutResvTask(customerId);
            if (atptResvTaskState.isTaskSuccess(customerId)) {
                String log = getPartionText(context.self().id())
                        + " OrderFn: attempt reservation success, customerId: " + customerId;
                showLogPrt(log);
                // set anync task state
                atptResvTaskState.addNewTask(customerId, checkoutResvs.size(), Enums.TaskType.ConfirmReservationsType);
                onDecideReservations(context, checkoutResvs, Enums.TaskType.ConfirmReservationsType);
            } else {
                String log = getPartionText(context.self().id())
                        + " OrderFn: attempt reservation failed, customerId: " + customerId;
                showLogPrt(log);
                List<CheckoutResv> checkoutResvsSuccessSub = atptResvTaskState.getSuccessAttempResvSubtask(customerId);


                if (checkoutResvsSuccessSub.size() == 0) {
                    // bug fix, 可能每一项库存都不足
                    String log_ = getPartionText(context.self().id())
                            + " @@@@ OrderFn: cancel reservation finish, customerId: " + customerId + "\n";
                    showLogPrt(log_);
                    sendMessage(context,
                            CartFn.TYPE,
                            String.valueOf(customerId),
                            CheckoutCartResult.TYPE,
                            new CheckoutCartResult(false));

                    //  remove checkout temp info
                    OrderTempInfoState orderTempInfoState = getTempCKInfoState(context);
                    Checkout checkout = orderTempInfoState.getSingleCheckout(customerId);

                    processFailOrder(context, checkout); // next step: paymentFn (nothing to dp with it in this case)

                    orderTempInfoState.removeSingleCheckout(customerId);
                    context.storage().set(TEMPCKINFOSTATE, orderTempInfoState);
                } else {
                    // set anync task state
                    atptResvTaskState.addNewTask(customerId, checkoutResvsSuccessSub.size(), Enums.TaskType.CancelReservationsType);
                    onDecideReservations(context, checkoutResvsSuccessSub, Enums.TaskType.CancelReservationsType);
                }
            }
            atptResvTaskState.removeTask(customerId, Enums.TaskType.AttemptReservationsType);
        }
        context.storage().set(ASYNCTASKSTATE, atptResvTaskState);
    }

//    last step of case 1: confirm reservation
    private void dealConfirmResponse(Context context, Long customerId) {
        OrderAsyncTaskState atptResvTaskState = getAtptResvTaskState(context);
        atptResvTaskState.addCompletedSubTask(customerId, null, Enums.TaskType.ConfirmReservationsType);
        boolean isTaskComplete = atptResvTaskState.isTaskComplete(customerId, Enums.TaskType.ConfirmReservationsType);
        if (isTaskComplete) {
            String log = getPartionText(context.self().id())
                    + " @@@@ OrderFn: confirm reservation finish, customerId: " + customerId + "\n";
            showLogPrt(log);
//            // TODO: 6/6/2023  do something , no its not the time to finsih
//            sendMessage(context,
//                    CartFn.TYPE,
//                    String.valueOf(customerId),
//                    CheckoutCartResult.TYPE,
//                    new CheckoutCartResult(true));

            //  remove checkout temp info and asyncTask info,
            OrderTempInfoState orderTempInfoState = getTempCKInfoState(context);
            Checkout checkout = orderTempInfoState.getSingleCheckout(customerId);

            processSuccessOrder(context, checkout); // next step: paymentFn -> payment and save history

            orderTempInfoState.removeSingleCheckout(customerId);
            atptResvTaskState.removeTask(customerId, Enums.TaskType.CancelReservationsType);
            context.storage().set(TEMPCKINFOSTATE, orderTempInfoState);
        }
        context.storage().set(ASYNCTASKSTATE, atptResvTaskState);
    }

//    last step of case 2: cancel reservation
    private void dealCancelResponse(Context context, Long customerId) {
        OrderAsyncTaskState atptResvTaskState = getAtptResvTaskState(context);
        atptResvTaskState.addCompletedSubTask(customerId, null, Enums.TaskType.CancelReservationsType);
        boolean isTaskComplete = atptResvTaskState.isTaskComplete(customerId, Enums.TaskType.CancelReservationsType);
        if (isTaskComplete) {

            String log = getPartionText(context.self().id())
                    + " @@@@ OrderFn: cancel reservation finish, customerId: " + customerId + "\n";
            showLogPrt(log);
//            notify cartFn
            sendMessage(context,
                    CartFn.TYPE,
                    String.valueOf(customerId),
                    CheckoutCartResult.TYPE,
                    new CheckoutCartResult(false));

//          remove checkout temp info and asyncTask info,
            OrderTempInfoState orderTempInfoState = getTempCKInfoState(context);
            Checkout checkout = orderTempInfoState.getSingleCheckout(customerId);

            processFailOrder(context, checkout); // next step: paymentFn (nothing to dp with it in this case)

            orderTempInfoState.removeSingleCheckout(customerId);
            atptResvTaskState.removeTask(customerId, Enums.TaskType.CancelReservationsType);
            context.storage().set(TEMPCKINFOSTATE, orderTempInfoState);
        }
        context.storage().set(ASYNCTASKSTATE, atptResvTaskState);
    }

//    =================================================================================
//    After we handle the confirmation or cancellation of an order with stockFn,
//    Handing over to paymentFn for processing.
//    =================================================================================

    private void processSuccessOrder(Context context, Checkout checkout) {
        long orderId = generateNextOrderID(context);

        //        calculate total amount
        BigDecimal total_amount = BigDecimal.ZERO;
        Map<Long, BasketItem> items = checkout.getItems();
        for (Map.Entry<Long, BasketItem> entry : items.entrySet()) {
            BasketItem item = entry.getValue();
            BigDecimal price = BigDecimal.valueOf(item.getUnitPrice());
            int quantity = item.getQuantity();
            BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
            total_amount = total_amount.add(amount);
        }

        Order successOrder = new Order();
        successOrder.setId(orderId);
        successOrder.setCustomerId(checkout.getCustomerCheckout().getCustomerId());
//      invoice is a request for payment, so it makes sense to use this status now
        successOrder.setStatus(Enums.OrderStatus.INVOICED);
        successOrder.setPurchaseTimestamp(checkout.getCreatedAt());
        successOrder.setCreated_at(LocalDateTime.now());
        successOrder.setData(checkout.toString());
        successOrder.setTotalAmount(total_amount);
        successOrder.setCountItems(checkout.getItems().size());

//        add order and orderHistory to orderState
        OrderState orderState = getOrderState(context);
        orderState.addOrder(orderId, successOrder);

//        Map<Long, Order> orders = orderState.getOrders();
//        TreeMap<Long, List<OrderHistory>> orderHistories = orderState.getOrderHistory();
        long historyId = generateNextOrderHistoryID(context);
        LocalDateTime now = LocalDateTime.now();

//        orders.put(orderId, successOrder);
        OrderHistory orderHistory = new OrderHistory(historyId, now, Enums.OrderStatus.INVOICED);
//        orderHistories.put(orderId, new ArrayList<>());
//        orderHistories.get(orderId).add(orderHistory);
        orderState.addOrderHistory(orderId, orderHistory);
        context.storage().set(ORDERSTATE, orderState);

        List<OrderItem> orderItems = new ArrayList<>();
        int i = 0;
        for (Map.Entry<Long, BasketItem> entry : items.entrySet()) {
            BasketItem item = entry.getValue();
            orderItems.add(new OrderItem(
                orderId,
                i,
                item.getProductId(),
                item.getSellerId(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getQuantity() * item.getUnitPrice()
            ));
            i++;
        }

        Invoice invoice = new Invoice(
                checkout.getCustomerCheckout(),
                successOrder,
                orderItems,
                context.self().id()
        );

//
        long paymentPation = orderId % Constants.nPaymentPartitions;
        sendMessage(context,
                PaymentFn.TYPE,
                String.valueOf(paymentPation),
                ProcessPayment.TYPE,
                new ProcessPayment(invoice));
    }

    private void processFailOrder(Context context, Checkout checkout) {
        // assuming most succeed, overhead is not too high
        long orderId = generateNextOrderID(context);

        Order failedOrder = new Order();
        failedOrder.setId(orderId);
        failedOrder.setCustomerId(checkout.getCustomerCheckout().getCustomerId());
        failedOrder.setStatus(Enums.OrderStatus.CANCLED);
        failedOrder.setPurchaseTimestamp(checkout.getCreatedAt());
        failedOrder.setCreated_at(LocalDateTime.now());
        failedOrder.setData(checkout.toString());

        OrderState orderState = getOrderState(context);
        orderState.addOrder(orderId, failedOrder);
        context.storage().set(ORDERSTATE, orderState);

//       这一步在c#中是paymentFn处理的，但这里为了省去大量的消息久直接在这处理了，上面的successOrder不需要这样
        UpdateOrderStatus(context, orderId, Enums.OrderStatus.CANCLED);

        long customerId = checkout.getCustomerCheckout().getCustomerId();
        sendMessage(context,
                CustomerFn.TYPE,
                String.valueOf(customerId % Constants.nCustomerPartitions),
                NotifyCustomer.TYPE,
                new NotifyCustomer(customerId, failedOrder, Enums.NotificationType.notify_failed_payment));

        // also, its time to finsh the checkout process, notift cartFn
        sendMessage(context,
            CartFn.TYPE,
            String.valueOf(customerId),
            CheckoutCartResult.TYPE,
            new CheckoutCartResult(false));
    }

    private void UpdateOrderStatus(Context context, long orderId, Enums.OrderStatus status) {
        OrderState orderState = getOrderState(context);
        Map<Long, Order> orders = orderState.getOrders();
        TreeMap<Long, List<OrderHistory>> orderHistories = orderState.getOrderHistory();

        if (!orders.containsKey(orderId)) {
            String str = new StringBuilder().append("Order ").append(orderId)
                    .append(" cannot be found to update to status ").append(status.toString()).toString();
            throw new RuntimeException(str);
        }
        
        LocalDateTime now = LocalDateTime.now();

        orders.get(orderId).setUpdated_at(now);
        Enums.OrderStatus oldStatus = orders.get(orderId).getStatus();
        orders.get(orderId).setStatus(status);

        switch (status) {
            case SHIPPED:
                orders.get(orderId).setDelivered_carrier_date(now);
                break;
            case DELIVERED:
                orders.get(orderId).setDelivered_customer_date(now);
                break;
            case CANCLED:
            case PAYMENT_FAILED:
            case PAYMENT_SUCCESS:
                orders.get(orderId).setPaymentDate(now);
                break;
            default:
                break;
        }

        if (status != Enums.OrderStatus.CANCLED) {
            long historyId = generateNextOrderHistoryID(context);
            OrderHistory orderHistory = new OrderHistory(historyId, now, status);
            orderHistories.get(orderId).add(orderHistory);
        }

        context.storage().set(ORDERSTATE, orderState);

        String log = getPartionText(context.self().id())
                + "update order status, orderId: " + orderId + ", oldStatus: " + oldStatus + ", newStatus: " + status + "\n";
        showLog(log);
    }
}