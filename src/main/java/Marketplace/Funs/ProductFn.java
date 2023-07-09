package Marketplace.Funs;

import Common.Entity.Product;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToProdFn.GetAllProducts;
import Marketplace.Types.MsgToProdFn.UpdateSinglePrice;
import Marketplace.Types.MsgToSeller.*;
import Marketplace.Types.MsgToProdFn.GetProduct;
import Marketplace.Types.State.ProductState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.io.KafkaEgressMessage;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

//import static Marketplace.Types.Messages.RESERVATIONRESULT_TYPE;

public class ProductFn implements StatefulFunction {

    Logger logger = Logger.getLogger("ProductFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "product");

    static final ValueSpec<ProductState> PRODUCTSTATE = ValueSpec.named("product").withCustomType(ProductState.TYPE);

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(PRODUCTSTATE)
            .withSupplier(ProductFn::new)
            .build();

    private static final TypeName ECOMMERCE_EGRESS = TypeName.typeNameOf(Constants.EGRESS_NAMESPACE, "egress");
    static final TypeName KFK_EGRESS = TypeName.typeNameOf("e-commerce.fns", "kafkaSink");

    private String getPartionText(String id) {
        return String.format("[ ProductFn partitionId %s ] ", id);
    }
    private String getPartionTextInline(String id) {
        return String.format("\n[ ProductFn partitionId %s ] ", id);
    }

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try{
            // seller --> product (increase stock)
//            if (message.is(IncreaseStock.TYPE)) {
//                onIncreaseStockAsyncCheck(context, message);
//                // client --> product (get product)
//            }
            if (message.is(GetProduct.TYPE)) {
                onGetProduct(context, message);
            }
            // seller --> product (getAllProducts of seller)
            else if (message.is(GetAllProducts.TYPE)) {
                onGetAllProducts(context, message);
            }
            // seller --> product (add product)
            else if (message.is(AddProduct.TYPE)) {
                onAddProduct(context, message);
            }
            // driver --> product (delete product)
            else if (message.is(DeleteProduct.TYPE)) {
                onDeleteProduct(context, message);
            }
            // driver --> product (update price)
            else if (message.is(UpdateSinglePrice.TYPE)) {
                onUpdatePrice(context, message);
            }
        } catch (Exception e) {
            System.out.println("Exception in ProductFn !!!!!!!!!!!!!");
            e.printStackTrace();
        }
        return context.done();
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private void printLog(String log) {
        System.out.println(log);
    }

    private ProductState getProductState(Context context) {
        return context.storage().get(PRODUCTSTATE).orElse(new ProductState());
    }

//    private void onIncreaseStockAsyncCheck(Context context, Message message) {
//        IncreaseStock increaseStock = message.as(IncreaseStock.TYPE);
//        Long productId = increaseStock.getStockItem().getProduct_id();
//        ProductState productState = getProductState(context);
//        Product product = productState.getProduct(productId);
//        IncreaseStockChkProd increaseStockChkProd = new IncreaseStockChkProd(increaseStock, product);
//        sendCheckResToSeller(context, increaseStockChkProd);
//    }

    private void onGetAllProducts(Context context, Message message) {
        ProductState productState = getProductState(context);
        GetAllProducts getAllProducts = message.as(GetAllProducts.TYPE);
        Long sellerId = getAllProducts.getSeller_id();
        Product[] products = productState.getProductsOfSeller(sellerId);
        sendTaskResToSeller(context, products, Enums.TaskType.GetAllProductsType);
        String log = String.format(getPartionTextInline(context.self().id())
                + " #sub-task# "
                + "get all products belongs to seller success, "
                + "number of products = " + (products.length)
                , sellerId);
        showLog(log);
    }

    private void onGetProduct(Context context, Message message) {
        ProductState productState = getProductState(context);
        GetProduct getProduct = message.as(GetProduct.TYPE);
        Long productId = getProduct.getProduct_id();
        Product product = productState.getProduct(productId);
        if (product == null) {
            String log = String.format(getPartionText(context.self().id())
                    + "get product failed as product not exist\n"
                    + "product id = " + productId
                    + "\n");
            showLog(log);
            return;
        }
        String log = String.format(getPartionText(context.self().id())
                + "get product success\n"
                + product.toString()
                + "\n");

        showLog(log);
    }

    private void onAddProduct(Context context, Message message) {
        ProductState productState = getProductState(context);
        AddProduct addProduct = message.as(AddProduct.TYPE);
        Product product = addProduct.getProduct();
        productState.addProduct(product);
        context.storage().set(PRODUCTSTATE, productState);

        String log = getPartionText(context.self().id())
//                + " #sub-task# "
                + "add product success, " + "product Id : " + product.getProduct_id() + "\n";
        printLog(log);
//        sendTaskResToSeller(context, product, Enums.TaskType.AddProductType);
    }

    private void onDeleteProduct(Context context, Message message) {
        DeleteProduct deleteProduct = message.as(DeleteProduct.TYPE);
        Long productId = deleteProduct.getProduct_id();

        ProductState productState = getProductState(context);
        Product product = productState.getProduct(productId);
        if (product == null) {
            String log = getPartionText(context.self().id())
                    + "delete product failed as product not exist\n"
                    + "product Id : " + productId
                    + "\n";
//            showLog(log);
            logger.warning(log);
            return;
        }
        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());

        context.storage().set(PRODUCTSTATE, productState);

        String log = getPartionText(context.self().id())
                + "delete product success\n"
                + "product Id : " + productId
                + "\n";
        showLog(log);

        String stockFnPartitionID = String.valueOf((int) (productId % Constants.nStockPartitions));
        sendMessage(context, StockFn.TYPE, stockFnPartitionID, DeleteProduct.TYPE, deleteProduct);
    }

    private void onUpdatePrice(Context context, Message message) {
        UpdateSinglePrice updatePrice = message.as(UpdateSinglePrice.TYPE);
        Long productId = updatePrice.getProductId();

        ProductState productState = getProductState(context);
        Product product = productState.getProduct(productId);
        if (product == null) {
            String log = getPartionText(context.self().id())
                    + "update price failed as product not exist\n"
                    + "product Id : " + productId
                    + "\n";
            logger.warning(log);
            return;
        }
        product.setPrice(updatePrice.getPrice());
        product.setUpdatedAt(LocalDateTime.now());
        context.storage().set(PRODUCTSTATE, productState);

        String log = getPartionText(context.self().id())
                + "update product success\n"
                + "product Id : " + product.getProduct_id()
                + " new price : " + product.getPrice()
                + "\n";
        showLog(log);
        context.send(
                KafkaEgressMessage.forEgress(KFK_EGRESS)
                        .withTopic("updatePriceTask")
                        .withUtf8Key(context.self().id())
                        .withUtf8Value("update product done (ID: " + productId + ")")
                        .build());
//        sendTaskResToSeller(context, product, Enums.TaskType.UpdatePriceType);
    }

//    private void sendCheckResToSeller(Context context, IncreaseStockChkProd increaseStockChkProd) {
//        final Optional<Address> caller = context.caller();
//        if (caller.isPresent()) {
//            context.send(
//                    MessageBuilder.forAddress(caller.get())
//                            .withCustomType(IncreaseStockChkProd.TYPE, increaseStockChkProd)
//                            .build());
//        } else {
//            throw new IllegalStateException("There should always be a caller.");
//        }
//    }

    private void sendTaskResToSeller(Context context, Product product, Enums.TaskType taskType) {
        final Optional<Address> caller = context.caller();
        if (caller.isPresent()) {
            TaskFinish taskFinish = new TaskFinish(taskType, Enums.SendType.ProductFn, product.getProduct_id());
            context.send(
                    MessageBuilder.forAddress(caller.get())
                            .withCustomType(TaskFinish.TYPE, taskFinish)
                            .build());
        } else {
            throw new IllegalStateException("There should always be a caller.");
        }
    }

//    this is special for getAllProducts of a seller
    private void sendTaskResToSeller(Context context, Product[] products, Enums.TaskType taskType) {
        final Optional<Address> caller = context.caller();
        if (caller.isPresent()) {
            TaskFinish taskFinish = new TaskFinish(taskType, Enums.SendType.ProductFn, -1L);
            taskFinish.setProductsOfSeller(products);
            context.send(
                    MessageBuilder.forAddress(caller.get())
                            .withCustomType(TaskFinish.TYPE, taskFinish)
                            .build());
        } else {
            throw new IllegalStateException("There should always be a caller.");
        }
    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }
}
