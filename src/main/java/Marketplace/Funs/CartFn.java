package Marketplace.Funs;

import java.util.logging.Logger;

import Common.Entity.BasketItem;
import Common.Entity.CustomerCheckout;
import Common.Entity.KafkaResponse;
import Marketplace.Constant.Constants;
import Common.Entity.Checkout;

import Marketplace.Types.MsgToCartFn.*;

import Marketplace.Types.State.CartState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.io.KafkaEgressMessage;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class CartFn implements StatefulFunction {

    Logger logger = Logger.getLogger("CartFn");

    // Statefun Type ，Logical name = <namespace> + <name>
    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "cart");

    //    static final ValueSpec<Long> CUSTOMERID = ValueSpec.named("customerId").withLongType();
    static final ValueSpec<CartState> CARTSTATE = ValueSpec.named("cartState").withCustomType(CartState.TYPE);

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(CARTSTATE)
            .withSupplier(CartFn::new)
            .build();

    static final TypeName KFK_EGRESS = TypeName.typeNameOf("e-commerce.fns", "kafkaSink");

    // for GET_CART_TYPE request, get items in cart and send to egress
    private static final TypeName ECOMMERCE_EGRESS = TypeName.typeNameOf(Constants.EGRESS_NAMESPACE, "egress");

    private String getPartionText(String id) {
        return String.format("[ CartFn partitionId %s ] ", id);
    }

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            // client ---> cart (add some item to cart)
            if (message.is(AddToCart.TYPE)) {
                onAddToCart(context, message);
            }
            // client ---> cart (send checkout request)
            else if (message.is(CheckoutCart.TYPE)) {
                onCheckoutAsyncBegin(context, message);
            }
            // client ---> cart (clear cart)
            else if (message.is(ClearCart.TYPE)) {
                onClearCart(context);
            }
            // order ---> cart (send checkout result)
            else if (message.is(CheckoutCartResult.TYPE)) {
                onCheckoutCartResult(context, message);
            }
            // client ---> cart (get cart content)
            else if (message.is(GetCart.TYPE)) {
                onGetCart(context);
            }
        } catch (Exception e) {
//            System.out.println("Exception in CartFn !!!!!!!!!!!!!!!!");
//            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return context.done();
    }

    private CartState getCartState(Context context) {
        return context.storage().get(CARTSTATE).orElse(new CartState());
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private void showLogPrt(String log) {
//        logger.info(log);
        System.out.println(log);
    }

    private void onAddToCart(Context context, Message message) {
        CartState cartState = getCartState(context);
        AddToCart addToCart = message.as(AddToCart.TYPE);

        BasketItem item = new BasketItem(
            addToCart.getSellerId(),
            addToCart.getProductId(),
            addToCart.getProductName(),
            addToCart.getUnitPrice(),
            addToCart.getFreightValue(),
            addToCart.getQuantity(),
                addToCart.getVouchers()
        );

//        BasketItem item = addToCart.getItem();
        cartState.addItem(item.getProductId(), item);
        context.storage().set(CARTSTATE, cartState);

        String log = String.format(getPartionText(context.self().id())
                        + "Item {%s} add to cart success\n"
                , item.getProductId());
        showLog(log);
//        long sellerId = addToCart.getSellerId();
//        String workerID = context.self().id();
//        String response = "";
//        try {
        
//            // addToCart 比较特殊，没有tid，所以第一个参数taskId传的是productId，第二个参数传的是sellerId
//            KafkaResponse kafkaResponse = new KafkaResponse(addToCart.getProductId(), (int)sellerId, workerID, "success");
//            ObjectMapper mapper = new ObjectMapper();
//            response = mapper.writeValueAsString(kafkaResponse);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }

//        System.out.println(getPartionText(context.self().id())+" send addItemToCart response to kafka: " + response);
//        context.send(
//                KafkaEgressMessage.forEgress(KFK_EGRESS)
//                        .withTopic("addItemToCartTask")
//                        .withUtf8Key(context.self().id())
//                        .withUtf8Value(response)
//                        .build());
    }

    private void onCheckoutAsyncBegin(Context context, Message message) {
        CustomerCheckout customerCheckout = message.as(CheckoutCart.TYPE).getCustomerCheckout();

        int transactionId = customerCheckout.getInstanceId();
        CartState cartState = getCartState(context);
        cartState.setCurrentTransactionId(transactionId);
        String cartId = context.self().id();
        String customerId = String.valueOf(customerCheckout.getCustomerId());

        String log_ = getPartionText(context.self().id())
                + "checkout request received\n"
                + "cartId: " + cartId + "\n"
                + "customerId: " + customerId + "\n"
                + "instanceId: " + customerCheckout.getInstanceId() + "\n";

        if (cartState.getStatus() == CartState.Status.CHECKOUT_SENT ){
            String log = getPartionText(context.self().id())
                    + "checkout is in process....please wait\n";
            showLog(log);
            throw new RuntimeException(log);
        }
        if (cartState.getItems().isEmpty()) {
            String log = getPartionText(context.self().id())
                    + "checkout fail as cart is empty\n";
            showLog(log);
            throw new RuntimeException(log);
        }
        if(!cartId.equals(customerId)) {
            String log = getPartionText(context.self().id())
                    + "checkout fail as customer id is not match\n"
                    + "cartId: " + cartId + "\n"
                    + "customerId: " + customerId + "\n"
                    + "instanceId: " + customerCheckout.getInstanceId() + "\n";

            showLog(log);
            throw new RuntimeException(log);
        }

        Checkout checkout = new Checkout(LocalDateTime.now(), customerCheckout, cartState.getItems());

        // order is chose randomly !!!
        String orderPartitionId = String.valueOf((int) (Math.random() * Constants.nOrderPartitions));
        sendMessage(context, OrderFn.TYPE, orderPartitionId, Checkout.TYPE, checkout);

        cartState.setStatus(CartState.Status.CHECKOUT_SENT);
        context.storage().set(CARTSTATE, cartState);

        String log = getPartionText(context.self().id())
                + "checkout message send........\n";
        showLogPrt(log);
    }

    private void Seal(Context context){
        CartState cartState = getCartState(context);
        if (cartState.getStatus() == CartState.Status.CHECKOUT_SENT) {
            cartState.setStatus(CartState.Status.OPEN);
            cartState.clear();
            context.storage().set(CARTSTATE, cartState);
            String log = getPartionText(context.self().id())
                    + "checkout success, cart ID: " + context.self().id() + "\n";
            showLog(log);
        } else {
            System.out.println("Cannot seal a cart that has not been checked out");
        }
    }

    private void onCheckoutCartResult(Context context, Message message) throws Exception {
        CartState cartState = getCartState(context);
        CheckoutCartResult checkoutCartResult = message.as(CheckoutCartResult.TYPE);
        String cartId = context.self().id();
        String result = "fail";

        if (checkoutCartResult.isSuccess()) {
            Seal(context);
            result = "success";
        } else {
            if (cartState.getStatus() == CartState.Status.CHECKOUT_SENT) {
                cartState.setStatus(CartState.Status.OPEN);
                context.storage().set(CARTSTATE, cartState);
            }
            String log = getPartionText(cartId)
                    + "checkout fail" + cartId + "\n";
            showLog(log);
        }

        String response = "";
        try {
            KafkaResponse kafkaResponse = new KafkaResponse(
                    Long.parseLong(cartId),
                    cartState.getCurrentTransactionId(),
                    cartId,
                    result);
            ObjectMapper mapper = new ObjectMapper();
            response = mapper.writeValueAsString(kafkaResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        System.out.println(getPartionText(context.self().id())+" send checkout response to kafka: " + response);
        context.send(
                KafkaEgressMessage.forEgress(KFK_EGRESS)
                        .withTopic("checkoutTask")
                        .withUtf8Key(context.self().id())
                        .withUtf8Value(response)
                        .build());
//        System.out.println("checkout result send to kafka");
    }

    private void onClearCart(Context context) {
        final AddressScopedStorage storage = context.storage();
        storage
                .get(CARTSTATE)
                .ifPresent(
                        cartState -> {
                            cartState.clear();
                            storage.set(CARTSTATE, cartState);
                        });
        logger.info(String.format("clear cart {%s} success", context.self().id()));
    }


    private void onGetCart(Context context) {
        CartState cartState = getCartState(context);

        String log = String.format(getPartionText(context.self().id())
                        + "get cart success\n"
                        + "cart status: {%s}\n"
                        + "cart content: {\n%s}\n"
                , cartState.getStatus(), cartState.getCartConent());
        showLog(log);
    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }
}
