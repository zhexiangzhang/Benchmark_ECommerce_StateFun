package Marketplace.Funs;

import java.util.logging.Logger;

import Common.Entity.BasketItem;
import Common.Entity.CustomerCheckout;
import Marketplace.Constant.Constants;
import Common.Entity.Checkout;

import Marketplace.Types.MsgToCartFn.*;

import Marketplace.Types.State.CartState;
import org.apache.flink.statefun.sdk.java.*;
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
            System.out.println("Exception in CartFn !!!!!!!!!!!!!!!!");
            e.printStackTrace();

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
        BasketItem item = addToCart.getItem();
        cartState.addItem(item.getProductId(), item);
        context.storage().set(CARTSTATE, cartState);

        String log = String.format(getPartionText(context.self().id())
                        + "Item {%s} add to cart success\n"
                , item.getProductId());
        showLog(log);
    }

    private void onCheckoutAsyncBegin(Context context, Message message) {
        CustomerCheckout customerCheckout = message.as(CheckoutCart.TYPE).getCustomerCheckout();

        CartState cartState = getCartState(context);
        String cartId = context.self().id();
        String customerId = String.valueOf(customerCheckout.getCustomerId());

        if (cartState.getStatus() == CartState.Status.CHECKOUT_SENT ){
            String log = getPartionText(context.self().id())
                    + "checkout is in process....please wait\n";
            showLog(log);
            return;
        }
        if (cartState.getItems().isEmpty()) {
            String log = getPartionText(context.self().id())
                    + "checkout fail as cart is empty\n";
            showLog(log);
            return;
        }
        if(!cartId.equals(customerId)) {
            String log = getPartionText(context.self().id())
                    + "checkout fail as customer id is not match\n";
            showLog(log);
            return;
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
        boolean isSuccess = checkoutCartResult.isSuccess();
        if (isSuccess) {
            Seal(context);
        } else {
            if (cartState.getStatus() == CartState.Status.CHECKOUT_SENT) {
                cartState.setStatus(CartState.Status.OPEN);
                context.storage().set(CARTSTATE, cartState);
            }
            String log = getPartionText(context.self().id())
                    + "checkout fail" + context.self().id() + "\n";
            showLog(log);
        }
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
