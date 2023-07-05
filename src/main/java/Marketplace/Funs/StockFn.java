package Marketplace.Funs;

import Common.Entity.BasketItem;
import Common.Entity.Product;
import Common.Entity.StockItem;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToSeller.AddProduct;
import Marketplace.Types.MsgToSeller.DeleteProduct;
import Marketplace.Types.MsgToSeller.IncreaseStock;
import Marketplace.Types.MsgToSeller.TaskFinish;
import Marketplace.Types.MsgToStock.CheckoutResv;
import Marketplace.Types.MsgToStock.PaymentResv;
import Marketplace.Types.State.StockState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;
import org.apache.flink.statefun.sdk.java.types.Types;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class StockFn implements StatefulFunction {

    Logger logger = Logger.getLogger("StockFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "stock");

    static final ValueSpec<StockState> STOCKSTATE = ValueSpec.named("stock").withCustomType(StockState.TYPE);

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(STOCKSTATE)
            .withSupplier(StockFn::new)
            .build();

    private String getPartionText(String id) {
        return String.format("[ StockFn partitionId %s ] ", id);
    }

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            // seller ---> stock (increase stock)
            if (message.is(IncreaseStock.TYPE)) {
                onIncreaseStock(context, message);
            }
            // seller ---> stock (add product)
            else if (message.is(AddProduct.TYPE)) {
                onAddItem(context, message);
            }
            // seller ---> stock (delete product)
            else if (message.is(DeleteProduct.TYPE)) {
                onDeleteItem(context, message);
            }
            // order ---> stock (attempt reservsation)
            else if (message.is(CheckoutResv.TYPE)) {
                onHandleCheckoutResv(context, message);
            }
            // payment ---> stock (payment result finally decided change stock or not
            else if (message.is(PaymentResv.TYPE)) {
                onHandlePaymentResv(context, message);
            }
        } catch (Exception e) {
            System.out.println("StockFn apply error !!!!!!!!!!!!!!!");
            e.printStackTrace();
        }

        return context.done();
    }

    private StockState getStockState(Context context) {
        return context.storage().get(STOCKSTATE).orElse(new StockState());
    }

    private void showLog(String log) {
//        logger.info(log);
        System.out.println(log);
    }

    private void onIncreaseStock(Context context, Message message) {
        IncreaseStock increaseStock = message.as(IncreaseStock.TYPE);
        StockItem stockItem = increaseStock.getStockItem();
        Long productId = stockItem.getProduct_id();

//        int num = stockItem.getQty_available();
        StockState stockState = getStockState(context);
        String stockChange = stockState.increaseStock(productId, stockItem);
        context.storage().set(STOCKSTATE, stockState);

        String log = String.format(getPartionText(context.self().id())
                        + "increaseStock success, "
                        + "productId: %s\n"
                , productId);
        showLog(log);

        String result = "IncreaseStock success, productId: " + productId + stockChange;
        System.out.println(result);
        sendMessageToCaller(context, Types.stringType(), result);
    }

    private void onAddItem(Context context, Message message) {
        StockState stockState = getStockState(context);
        AddProduct addProduct = message.as(AddProduct.TYPE);
        Product product = addProduct.getProduct();
        Long productId = product.getProduct_id();
        Long sellerId = product.getSeller_id();
        LocalDateTime time = LocalDateTime.now();
        StockItem stockItem = new StockItem(
                productId,
                sellerId,
                0,
                0,
                0,
                0,
                "");
        stockState.addItem(stockItem);
        context.storage().set(STOCKSTATE, stockState);

        String log = String.format(getPartionText(context.self().id())
                        + " #sub-task#"
                        + " addProduct success, productId: %s\n"
                , productId);
        showLog(log);

        sendTaskResToSeller(context, productId, Enums.TaskType.AddProductType);
    }

    private void onDeleteItem(Context context, Message message) {
        StockState stockState = getStockState(context);
        DeleteProduct deleteProduct = message.as(DeleteProduct.TYPE);
        Long productId = deleteProduct.getProduct_id();
        StockItem stockItem = stockState.getItem(productId);
        if (stockItem == null) {
            String log = String.format(getPartionText(context.self().id())
                            + "deleteItem failed as product not exist\n"
                            + "productId: %s\n"
                    , productId);
            showLog(log);
            return;
        }
        stockItem.setUpdatedAt(LocalDateTime.now());
        stockItem.setIs_active(false);
        context.storage().set(STOCKSTATE, stockState);

        String log = String.format(getPartionText(context.self().id())
                        + "deleteItem success\n"
                        + "productId: %s\n"
                , productId);
        showLog(log);

        sendTaskResToSeller(context, productId, Enums.TaskType.DeleteProductType);
    }

    private void onHandleCheckoutResv(Context context, Message message) {
        CheckoutResv checkoutResv = message.as(CheckoutResv.TYPE);

        BasketItem basketItem = checkoutResv.getItem();
        long productId = basketItem.getProductId();
        int quantity = basketItem.getQuantity();
        long customerId = checkoutResv.getCustomerId();

        Enums.TaskType taskType = checkoutResv.getTaskType();

        System.out.println("StockFn onHandleCheckoutResv, checkoutResv: " + checkoutResv.getTaskType());

        switch (taskType) {
            case AttemptReservationsType:
                Enums.ItemStatus itemStatus = onAtptResvReq(context, productId, quantity, customerId);
                checkoutResv.setItemStatus(itemStatus);
                break;
            case CancelReservationsType:
                onCancelResvReq(context, productId, quantity);
                break;
            case ConfirmReservationsType:
                onConfirmResvReq(context, productId, quantity);
                break;
            default:
                break;
        }


        sendMessageToCaller(
                context,
                CheckoutResv.TYPE,
                checkoutResv);
    }

    private Enums.ItemStatus onAtptResvReq(Context context, long productId, int quantity, long customerId) {
        StockState stockState = getStockState(context);
        StockItem stockItem = stockState.getItem(productId);

        String partitionText = getPartionText(context.self().id());
        String productIdText = "productId: " + productId;

        if (!stockItem.getIs_active()) {
            String log = partitionText + " #sub-task#, attempt reservation request failed as product not active\n"
                    + productIdText
                    + ", " + "customerId: " + customerId + "\n";
            showLog(log);
            return Enums.ItemStatus.DELETED;
        }
        if (stockItem.getQty_available() - stockItem.getQty_reserved() < quantity) {
            String log = partitionText + " #sub-task#, attempt reservation request failed as stock not enough\n"
                    + productIdText
                    + ", " + "customerId: " + customerId
                    + ", " + "qty_available: " + stockItem.getQty_available()
                    + ", " + "need: " + quantity + "\n";

            showLog(log);
            return Enums.ItemStatus.OUT_OF_STOCK;
        } else {
            stockItem.setQty_reserved(stockItem.getQty_reserved() + quantity);
            stockItem.setUpdatedAt(LocalDateTime.now());
            context.storage().set(STOCKSTATE, stockState);
            String log = partitionText + " #sub-task#, attempt reservation request success\n"
                    + productIdText
                    + ", " + "customerId: " + customerId
                    + ", " + "qty_available: " + stockItem.getQty_available()
                    + ", need: " + quantity + "\n";
            showLog(log);
            return Enums.ItemStatus.IN_STOCK;
        }
    }

    private void onCancelResvReq(Context context, long productId, int quantity) {
        StockState stockState = getStockState(context);
        StockItem stockItem = stockState.getItem(productId);
        stockItem.setQty_reserved(stockItem.getQty_reserved() - quantity);
        stockItem.setUpdatedAt(LocalDateTime.now());
        context.storage().set(STOCKSTATE, stockState);
    }

    private void onConfirmResvReq(Context context, long productId, int quantity) {
        StockState stockState = getStockState(context);
        StockItem stockItem = stockState.getItem(productId);
        stockItem.setQty_available(stockItem.getQty_available() - quantity);
        stockItem.setQty_reserved(stockItem.getQty_reserved() - quantity);
        stockItem.setUpdatedAt(LocalDateTime.now());
        context.storage().set(STOCKSTATE, stockState);
    }

    private void paymentFail(Context context, long productId, int quantity) {
        onCancelResvReq(context, productId, quantity);
    }

    private void paymentConfirm(Context context, long productId, int quantity) {
        // increase order count
        StockState stockState = getStockState(context);
        StockItem stockItem = stockState.getItem(productId);
        stockItem.setOrder_count(stockItem.getOrder_count() + 1);
        stockItem.setUpdatedAt(LocalDateTime.now());
        context.storage().set(STOCKSTATE, stockState);
    }

    private void onHandlePaymentResv(Context context, Message message){
        PaymentResv paymentResv = message.as(PaymentResv.TYPE);
        long productId = paymentResv.getProductId();
        int quantity = paymentResv.getQuantity();
        Enums.OrderStatus orderStatus = paymentResv.getOrderStatus();
        String uniqueId = paymentResv.getUniqueOrderId();
//        long orderId = paymentResv.getOrderId();

        String log = String.format(getPartionText(context.self().id())
                + "StockFn apply PaymentResv, productId: %s, uniqueOrderId: %s", productId, uniqueId);
        showLog(log);

        if (orderStatus == Enums.OrderStatus.PAYMENT_SUCCESS) {
//            NOTE: NOT call onConfirmResvReq here
            paymentConfirm(context, productId, quantity);
        } else {
            paymentFail(context, productId, quantity);
        }
//        reuse same message type (send to payment service)
        sendMessageToCaller(context, PaymentResv.TYPE, paymentResv);
    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }

    private <T> void sendMessageToCaller(Context context, Type<T> messageType, T messageContent) {
        final Optional<Address> caller = context.caller();
        if (caller.isPresent()) {
            context.send(
                    MessageBuilder.forAddress(caller.get())
                            .withCustomType(messageType, messageContent)
                            .build());
        } else {
            throw new IllegalStateException("There should always be a caller");
        }
    }

    private void sendTaskResToSeller(Context context, Long productId, Enums.TaskType taskType) {
        final Optional<Address> caller = context.caller();
        if (caller.isPresent()) {
            TaskFinish taskFinish = new TaskFinish(taskType, Enums.SendType.StockFn, productId);
            context.send(
                    MessageBuilder.forAddress(caller.get())
                            .withCustomType(TaskFinish.TYPE, taskFinish)
                            .build());
        } else {
            throw new IllegalStateException("There should always be a caller.");
        }
    }


}
