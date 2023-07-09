package Marketplace.Funs;

import Common.Entity.*;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
//import Marketplace.DummyPaymentServiceService;
//import Marketplace.PaymentService;
import Marketplace.Types.Messages;
import Marketplace.Types.MsgToCartFn.CheckoutCartResult;
import Marketplace.Types.MsgToCustomer.NotifyCustomer;
import Marketplace.Types.MsgToOrderFn.OrderStateUpdate;
import Marketplace.Types.MsgToPaymentFn.ProcessPayment;
import Marketplace.Types.MsgToShipment.ProcessShipment;
import Marketplace.Types.MsgToStock.CheckoutResv;
import Marketplace.Types.MsgToStock.PaymentResv;
import Marketplace.Types.State.PaymentAsyncTaskState;
import Marketplace.Types.State.PaymentState;
import Marketplace.Types.State.StockState;
import jdk.nashorn.internal.runtime.regexp.joni.constants.StringType;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;
import org.apache.flink.statefun.sdk.java.types.Types;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class PaymentFn implements StatefulFunction {

    Logger logger = Logger.getLogger("PaymentFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "payment");

    static final ValueSpec<PaymentAsyncTaskState> PAYMENT_ASYNC_TASK_STATE =
            ValueSpec.named("paymentAsyncTaskState")
                    .withCustomType(PaymentAsyncTaskState.TYPE);
    static final ValueSpec<PaymentState> PAYMENT_STATE = ValueSpec.named("paymentState").withCustomType(PaymentState.TYPE);

    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withSupplier(PaymentFn::new)
            .withValueSpecs(PAYMENT_ASYNC_TASK_STATE, PAYMENT_STATE)
            .build();

//    PaymentService paymentService = new DummyPaymentServiceService();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            // stock --> payment (request to payment)
            if (message.is(ProcessPayment.TYPE)) {
                onProcessPayment(context, message);
            }
            // stock --> payment (stock has completed the final confirm, i.e. after pay success or fail)
            else if (message.is(PaymentResv.TYPE)){
                onStockFinalChangeDone(context, message);
            }

        } catch (Exception e) {
            System.out.println("PaymentFn Exception !!!!!!!!!!!!!!!!");
            e.printStackTrace();
        }
        return context.done();
    }

//    ========================================================================
//                             helper functions
//    ========================================================================

    private PaymentAsyncTaskState getPaymentAsyncTaskState(Context context) {
        return context.storage().get(PAYMENT_ASYNC_TASK_STATE).orElse(new PaymentAsyncTaskState());
    }

    private PaymentState getPaymentState(Context context) {
        return context.storage().get(PAYMENT_STATE).orElse(new PaymentState());
    }

    private String getPartionText(String id) {
        return String.format("[ PaymentFn partitionId %s ] ", id);
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

//    ========================================================================
//                             business logic
//    ========================================================================

    private void onProcessPayment(Context context, Message message) {
        ProcessPayment processPayment = message.as(ProcessPayment.TYPE);
        Invoice invoice = processPayment.getInvoice();
        CustomerCheckout customerCheckout = invoice.getCustomerCheckout();
        Order order = invoice.getOrder();
        BigDecimal total = order.getTotalAmount();

        String log = getPartionText(context.self().id())
                + " asking for payment for order: " + order.getId();
        showLogPrt(log);

        long orderId = order.getId();
        String orderPartition = invoice.getOrderPartitionID();
        String uniqueOrderID = orderPartition + "_" + orderId;

        // add mapping from orderId to customerId (for notification which customer)
        PaymentAsyncTaskState paymentAsyncTaskState_ = getPaymentAsyncTaskState(context);
        paymentAsyncTaskState_.addInvoice(uniqueOrderID, invoice);
        context.storage().set(PAYMENT_ASYNC_TASK_STATE, paymentAsyncTaskState_);

        // Call the asynchronous payment processing method
//        CompletableFuture<Boolean> approved = paymentService.ContactESP(customerCheckout, total);
//
//        approved.whenComplete(
//                (result, throwable) -> {
//                    // TODO: 6/12/2023  the code below need to be fixed, context in the field :
//                    // TODO: 6/12/2023 noFurtherModificationsAllowed
//                }
//        );

        // ========================================================================
        boolean result = true; // assume always success

        String log_ = getPartionText(context.self().id())
                + "contact ESP finished, payment result for order: " + uniqueOrderID + " is " + result;
        showLogPrt(log_);

        Enums.OrderStatus orderStatus =
                result ? Enums.OrderStatus.PAYMENT_SUCCESS : Enums.OrderStatus.PAYMENT_FAILED;

        // add payment info to PaymentState
        PaymentState paymentState = getPaymentState(context);
        OrderPayment orderPayment = new OrderPayment(
                uniqueOrderID,
                customerCheckout.getCardHolderName(),
                customerCheckout.getCardNumber(),
                customerCheckout.getCardExpiration()
        );

        paymentState.addOrderPayment(uniqueOrderID, orderPayment);
        context.storage().set(PAYMENT_STATE, paymentState);

        // add async task to PaymentAsyncTaskState
        PaymentAsyncTaskState paymentAsyncTaskState = getPaymentAsyncTaskState(context);
        paymentAsyncTaskState.addNewTask(uniqueOrderID, invoice.getItems().size());
        context.storage().set(PAYMENT_ASYNC_TASK_STATE, paymentAsyncTaskState);
        System.out.println("=======================================");
        // send message to stock

        for (OrderItem item : invoice.getItems()) {
            long partition = (item.getProductId() % Constants.nStockPartitions);
            sendMessage(
                    context,
                    StockFn.TYPE,
                    String.valueOf(partition),
                    PaymentResv.TYPE,
                    new PaymentResv(
                            uniqueOrderID,
                            item.getProductId(),
                            item.getQuantity(),
                            orderStatus)
            );
        }

        String log__ = getPartionText(context.self().id())
                + "send payment result to stock for order: " + uniqueOrderID
                + " sending finished \n";
        showLogPrt(log__);

    // ========================================================================

//        approved.whenComplete(
//                (result, throwable) -> {
//                    long orderId = order.getId();
//
//                    String log_ = getPartionText(context.self().id())
//                            + "contact ESP finished, payment result for order: " + orderId + " is " + result;
//                    showLogPrt(log_);
//
//                    Enums.OrderStatus orderStatus =
//                            result ? Enums.OrderStatus.PAYMENT_SUCCESS : Enums.OrderStatus.PAYMENT_FAILED;
//
//                    // add payment info to PaymentState
//                    PaymentState paymentState = getPaymentState(context);
//                    OrderPayment orderPayment = new OrderPayment(
//                            orderId,
//                            customerCheckout.getName(),
//                            customerCheckout.getCardNumber(),
//                            customerCheckout.getCardExpiration()
//                            );
//
//                    paymentState.addOrderPayment(orderId, orderPayment);
//                    context.storage().set(PAYMENT_STATE, paymentState);
//
//                    // add async task to PaymentAsyncTaskState
//                    PaymentAsyncTaskState paymentAsyncTaskState = getPaymentAsyncTaskState(context);
//                    paymentAsyncTaskState.addNewTask(orderId, invoice.getItems().size());
//                    context.storage().set(PAYMENT_ASYNC_TASK_STATE, paymentAsyncTaskState);
//                    System.out.println("=======================================");
//                    // send message to stock
//
//                    for (OrderItem item : invoice.getItems()) {
//                        long partition = (item.getProductId() % Constants.nStockPartitions);
//                        sendMessage(
//                                context,
//                                StockFn.TYPE,
//                                String.valueOf(partition),
//                                PaymentResv.TYPE,
//                                new PaymentResv(
//                                        orderId,
//                                        item.getProductId(),
//                                        item.getQuantity(),
//                                        orderStatus)
//                        );
//                    }
//
//                    String log__ = getPartionText(context.self().id())
//                            + "send payment result to stock for order: " + orderId
//                            + " sending finished \n";
//                    showLogPrt(log__);
//                }
//        );
    }

    private void onStockFinalChangeDone(Context context, Message message) {
        PaymentAsyncTaskState paymentAsyncTaskState = getPaymentAsyncTaskState(context);
        PaymentResv paymentResv = message.as(PaymentResv.TYPE);
        String uniqueOrderID = paymentResv.getUniqueOrderId();
//        String[] parts = uniqueOrderID.split("_");
//        long orderId = Long.parseLong(parts[1]);

        String log = getPartionText(context.self().id())
                + " #sub-tasks#, receive stockFinalChange response: " + paymentResv.toString() + "\n";
        showLogPrt(log);

        paymentAsyncTaskState.addCompletedSubTask(uniqueOrderID);
        if (paymentAsyncTaskState.isAllSubTaskCompleted(uniqueOrderID)) {
            String log_ = getPartionText(context.self().id())
                    + "receive all sub tasks stockFinalChange response for order: " + uniqueOrderID + "\n";
            showLogPrt(log_);

            paymentAsyncTaskState.removeTask(uniqueOrderID);

            Invoice invoice = paymentAsyncTaskState.getInvoice(uniqueOrderID);
            Order order = invoice.getOrder();
            long customerId = order.getCustomerId();
            long orderId = order.getId();
            paymentAsyncTaskState.removeSingleInvoice(uniqueOrderID);

            if (paymentResv.getOrderStatus() == Enums.OrderStatus.PAYMENT_SUCCESS) {

                String orderPartition = invoice.getOrderPartitionID();
                long shipmentPartition = orderId % Constants.nShipmentPartitions;

                sendMessage(context, OrderFn.TYPE, String.valueOf(orderPartition),
                        OrderStateUpdate.TYPE,
                        new OrderStateUpdate(orderId, Enums.OrderStatus.PAYMENT_SUCCESS)
                );

                sendMessage(context, CustomerFn.TYPE, String.valueOf(customerId % Constants.nCustomerPartitions),
                        NotifyCustomer.TYPE,
                        new NotifyCustomer(customerId, order, Enums.NotificationType.notify_success_payment)
                );

                sendMessage(context, ShipmentFn.TYPE, String.valueOf(shipmentPartition),
                        ProcessShipment.TYPE,
                        new ProcessShipment(invoice)
                );

                // this time the checkout is finished, send message to cartFn to change state
                sendMessage(context, CartFn.TYPE, String.valueOf(customerId),
                        CheckoutCartResult.TYPE,
                        new CheckoutCartResult(true));
            } else {
                // payment failed
                sendMessage(context, CustomerFn.TYPE, String.valueOf(customerId % Constants.nCustomerPartitions),
                        NotifyCustomer.TYPE,
                        new NotifyCustomer(customerId, order, Enums.NotificationType.notify_failed_payment)
                );

                String orderPartition = invoice.getOrderPartitionID();
                sendMessage(context, OrderFn.TYPE, String.valueOf(orderPartition),
                        OrderStateUpdate.TYPE,
                        new OrderStateUpdate(orderId, Enums.OrderStatus.PAYMENT_FAILED)
                );

                // this time the checkout is finished, send message to cartFn to change state
                sendMessage(context, CartFn.TYPE, String.valueOf(customerId),
                        CheckoutCartResult.TYPE,
                        new CheckoutCartResult(false));
            }

            String log__ = getPartionText(context.self().id())
                    + "payment all finished, message sent for order, customer and shipment: " + uniqueOrderID + "\n";
            showLog(log__);
        }
        context.storage().set(PAYMENT_ASYNC_TASK_STATE, paymentAsyncTaskState);
    }

}
