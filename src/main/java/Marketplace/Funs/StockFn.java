package Marketplace.Funs;

import Common.Entity.BasketItem;
import Common.Entity.TransactionMark;
import Common.Entity.StockItem;
import Common.Utils.Utils;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToSeller.DeleteProduct;
import Marketplace.Types.MsgToSeller.IncreaseStock;
import Marketplace.Types.MsgToStock.ReserveStockEvent;
import Marketplace.Types.MsgToStock.ConfirmStockEvent;
import Marketplace.Types.State.StockState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.io.KafkaEgressMessage;
import org.apache.flink.statefun.sdk.java.message.Message;

import java.time.LocalDateTime;
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

    static final TypeName KFK_EGRESS = TypeName.typeNameOf("e-commerce.fns", "kafkaSink");

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            // seller ---> stock (increase stock)
            if (message.is(IncreaseStock.TYPE)) {
                addStockItem(context, message);
            }
            // product ---> stock (delete product)
            else if (message.is(DeleteProduct.TYPE)) {
                onDeleteItem(context, message);
            }
            // order ---> stock (attempt reservsation)
            else if (message.is(ReserveStockEvent.TYPE)) {
                onHandleCheckoutResv(context, message);
            }
            // payment ---> stock (payment result finally decided change stock or not
            else if (message.is(ConfirmStockEvent.TYPE)) {
                onHandlePaymentResv(context, message);
            }
        } catch (Exception e) {
//            System.out.println("StockFn apply error !!!!!!!!!!!!!!!");
//            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return context.done();
    }

    private StockState getStockState(Context context) {
        return context.storage().get(STOCKSTATE).orElse(new StockState());
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private void printLog(String log) {
        System.out.println(log);
    }

    private void addStockItem(Context context, Message message) {
        IncreaseStock increaseStock = message.as(IncreaseStock.TYPE);
        StockItem stockItem = increaseStock.getStockItem();
        Long productId = stockItem.getProduct_id();

//        int num = stockItem.getQty_available();
        StockState stockState = getStockState(context);
        stockState.addStock(productId, stockItem);
        context.storage().set(STOCKSTATE, stockState);

        String log = String.format(getPartionText(context.self().id())
                        + "addStockItem success, "
                        + "productId: %s\n"
                , productId);
        printLog(log);
    }

    private void onDeleteItem(Context context, Message message) {
        StockState stockState = getStockState(context);
        DeleteProduct deleteProduct = message.as(DeleteProduct.TYPE);
        Long productId = deleteProduct.getProduct_id();
        StockItem stockItem = stockState.getItem(productId);
        String result = "fail";

        if (stockItem == null) {
            String log = String.format(getPartionText(context.self().id())
                            + "deleteItem failed as product not exist\n"
                            + "productId: %s\n"
                    , productId);
            logger.warning(log);
        } else {
            stockItem.setUpdatedAt(LocalDateTime.now());
            stockItem.setIs_active(false);
            result = "success";
            context.storage().set(STOCKSTATE, stockState);

            String log = String.format(getPartionText(context.self().id())
                            + "deleteItem success (stock part)\n"
                            + "productId: %s\n"
                    , productId);
//            showLog(log);
        }

        int tid = deleteProduct.getInstanceId();
        long sellerId = deleteProduct.getSeller_id();
        String response = "";
        try {
            TransactionMark transactionMark = new TransactionMark(productId, tid, String.valueOf(sellerId), result);
            ObjectMapper mapper = new ObjectMapper();
            response = mapper.writeValueAsString(transactionMark);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

//        System.out.println(getPartionText(context.self().id())+" send delete product response to kafka: " + response);
        context.send(
                KafkaEgressMessage.forEgress(KFK_EGRESS)
                        .withTopic("deleteProductTask")
                        .withUtf8Key(context.self().id())
                        .withUtf8Value(response)
                        .build());
        String log_ = getPartionText(context.self().id())
                + "delete product success, " + "tid : " + deleteProduct.getInstanceId() + "\n";
        printLog(log_);
//        logger.info("[success] {tid=" + deleteProduct.getInstanceId() + "} delete product, stockFn " + context.self().id());
    }

    private void onHandleCheckoutResv(Context context, Message message) {
        ReserveStockEvent reserveStockEvent = message.as(ReserveStockEvent.TYPE);

        BasketItem basketItem = reserveStockEvent.getItem();
        long productId = basketItem.getProductId();
        int quantity = basketItem.getQuantity();
        long customerId = reserveStockEvent.getCustomerId();

        Enums.ItemStatus itemStatus = onAtptResvReq(context, productId, quantity, customerId);
        reserveStockEvent.setItemStatus(itemStatus);

        Utils.sendMessageToCaller(
                context,
                ReserveStockEvent.TYPE,
                reserveStockEvent);
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
//            showLog(log);
            return Enums.ItemStatus.DELETED;
        }
        if (stockItem.getQty_available() - stockItem.getQty_reserved() < quantity) {
            String log = partitionText + " #sub-task#, attempt reservation request failed as stock not enough\n"
                    + productIdText
                    + ", " + "customerId: " + customerId
                    + ", " + "qty_available: " + stockItem.getQty_available()
                    + ", " + "need: " + quantity + "\n";

//            showLog(log);
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
//            showLog(log);
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

    private void paymentFail(Context context, long productId, int quantity) {
        onCancelResvReq(context, productId, quantity);
    }

    private void paymentConfirm(Context context, long productId, int quantity) {
        // increase order count
        StockState stockState = getStockState(context);
        StockItem stockItem = stockState.getItem(productId);
        // 之前忘写了
        stockItem.setQty_reserved(stockItem.getQty_reserved() - quantity);
        stockItem.setQty_available(stockItem.getQty_available() - quantity);
        stockItem.setOrder_count(stockItem.getOrder_count() + 1);
        stockItem.setUpdatedAt(LocalDateTime.now());
        context.storage().set(STOCKSTATE, stockState);
    }

    private void onHandlePaymentResv(Context context, Message message){
        ConfirmStockEvent confirmStockEvent = message.as(ConfirmStockEvent.TYPE);
        long productId = confirmStockEvent.getProductId();
        int quantity = confirmStockEvent.getQuantity();
        Enums.OrderStatus orderStatus = confirmStockEvent.getOrderStatus();
        String uniqueId = confirmStockEvent.getUniqueOrderId();
//        long orderId = paymentResv.getOrderId();

        String log = String.format(getPartionText(context.self().id())
                + "StockFn apply PaymentResv, productId: %s, uniqueOrderId: %s", productId, uniqueId);
//        showLog(log);

        if (orderStatus == Enums.OrderStatus.PAYMENT_PROCESSED) {
//            NOTE: NOT call onConfirmResvReq here
            paymentConfirm(context, productId, quantity);
        } else {
            paymentFail(context, productId, quantity);
        }
    }
}






