package Marketplace.Funs;

import Common.Utils.PostgreHelper;
import Common.Utils.Utils;
import Common.Entity.Product;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToProdFn.UpdateSinglePrice;
import Marketplace.Types.MsgToSeller.*;
import Marketplace.Types.State.ProductState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

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
//    static final TypeName KFK_EGRESS = TypeName.typeNameOf("e-commerce.fns", "kafkaSink");

    private String getPartionText(String id) {
        return String.format("[ ProductFn partitionId %s ] ", id);
    }
    private String getPartionTextInline(String id) {
        return String.format("\n[ ProductFn partitionId %s ] ", id);
    }

    private boolean isFirstInvoke = true;

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try{
//            if (message.is(GetProduct.TYPE)) {
//                onGetProduct(context, message);
//            }
            // seller --> product (add product)
            if (message.is(AddProduct.TYPE)) {
                onAddProduct(context, message);
            }
            // driver --> product (delete product)
            else if (message.is(UpdateProduct.TYPE)) {
                String log_ = getPartionText(context.self().id())
                        + "update product [receive], " + "tid : " + message.as(UpdateProduct.TYPE).getVersion() + "\n";
                printLog(log_);
                logger.info("receive update product, tid : " + message.as(UpdateProduct.TYPE).getVersion());
                onUpdateProduct(context, message);
            }
            // driver --> product (update price)
            else if (message.is(UpdateSinglePrice.TYPE)) {
                onUpdatePrice(context, message);
            }
            else {
                printLog("ProductFn received unknown message type: " + message);
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

    private void onUpdateProduct(Context context, Message message) {
        if (isFirstInvoke) {
            isFirstInvoke = false;
            Product product = new Product(
                    1,
                    2,
                    "test",
                    "1",
                    "1",
                    "1",
                    1,
                    1,
                    "1",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    0
            );

            ProductState productState = getProductState(context);
            productState.addProduct(product);
            context.storage().set(PRODUCTSTATE, productState);

            String log = getPartionText(context.self().id())
                    + "init product" + "\n";
            printLog(log);
        }


        UpdateProduct updateProduct = message.as(UpdateProduct.TYPE);
        int productId = updateProduct.getProduct_id();

        String log_ = getPartionText(context.self().id())
                + "update product [receive], " + "tid : " + updateProduct.getVersion() + "\n";
        printLog(log_);

        ProductState productState = getProductState(context);
        Product product = productState.getProduct();
        if (product == null) {
            String log = getPartionText(context.self().id())
                    + "update product failed as product not exist\n"
                    + "product Id : " + productId
                    + "\n";
//            showLog(log);
            logger.warning(log);
            return;
        }
        // todo : send transaction marker
        product.setVersion(updateProduct.getVersion());
        product.setUpdatedAt(LocalDateTime.now());

        context.storage().set(PRODUCTSTATE, productState);

        String log = getPartionText(context.self().id())
                + "delete product success AT PRODUCTFN\n"
                + "product Id : " + productId
                + "\n";
        showLog(log);

//        String stockFnPartitionID = String.valueOf((int) (productId % Constants.nStockPartitions));
        String stockFnPartitionID = String.valueOf(productId);
        Utils.sendMessage(context, StockFn.TYPE, stockFnPartitionID, UpdateProduct.TYPE, updateProduct);
    }

    private void onUpdatePrice(Context context, Message message) {
        UpdateSinglePrice updatePrice = message.as(UpdateSinglePrice.TYPE);
        int productId = updatePrice.getProductId();

//        logger.info("[receive] {tid=" + updatePrice.getInstanceId() + "} update product, productFn " + context.self().id());
        String log_ = getPartionText(context.self().id())
                + "update price [receive], " + "tid : " + updatePrice.getInstanceId() + "\n";
        printLog(log_);

        ProductState productState = getProductState(context);
        Product product = productState.getProduct();

        Enums.MarkStatus markStatus = Enums.MarkStatus.ERROR;
//        String result = "fail";
        if (product == null) {
            String log = getPartionText(context.self().id())
                    + "update price failed as product not exist\n"
                    + "product Id : " + productId
                    + "\n";
            logger.warning(log);
        } else {
            product.setPrice(updatePrice.getPrice());
            product.setUpdatedAt(LocalDateTime.now());
//            result = "success";
            markStatus = Enums.MarkStatus.SUCCESS;
            context.storage().set(PRODUCTSTATE, productState);

            String log = getPartionText(context.self().id())
                    + "update product success\n"
                    + "product Id : " + product.getProduct_id()
                    + " new price : " + product.getPrice()
                    + "\n";
//            showLog(log);
        }

        int tid = updatePrice.getInstanceId();
        int sellerId = updatePrice.getSellerId();

        Utils.notifyTransactionComplete(context,
                Enums.TransactionType.updatePriceTask.toString(),
                String.valueOf(context.self().id()),
                productId,
                tid,
                String.valueOf(sellerId),
                markStatus,
                "product");

        String log = getPartionText(context.self().id())
                + "update price [success], " + "tid : " + updatePrice.getInstanceId() + "\n";
        printLog(log);
    }
}
