package Marketplace.Funs;

import Common.Entity.TransactionMark;
import Common.Utils.Utils;
import Common.Entity.Product;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToCartFn.Cleanup;
import Marketplace.Types.MsgToProdFn.GetAllProducts;
import Marketplace.Types.MsgToProdFn.UpdateSinglePrice;
import Marketplace.Types.MsgToSeller.*;
import Marketplace.Types.MsgToProdFn.GetProduct;
import Marketplace.Types.State.ProductState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.io.KafkaEgressMessage;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;

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
            if (message.is(GetProduct.TYPE)) {
                onGetProduct(context, message);
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
//            else if (message.is(Cleanup.TYPE))
//            {
//                onCleanup(context);
//            }
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

//        showLog(log);
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


        String log_ = getPartionText(context.self().id())
                + "delete product [receive], " + "tid : " + deleteProduct.getInstanceId() + "\n";
        printLog(log_);
//        logger.info(" {tid=" + deleteProduct.getInstanceId() + "} delete product, productFn " + context.self().id());
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
//        showLog(log);

        String stockFnPartitionID = String.valueOf((int) (productId % Constants.nStockPartitions));
        Utils.sendMessage(context, StockFn.TYPE, stockFnPartitionID, DeleteProduct.TYPE, deleteProduct);
    }

    private void onUpdatePrice(Context context, Message message) {
        UpdateSinglePrice updatePrice = message.as(UpdateSinglePrice.TYPE);
        Long productId = updatePrice.getProductId();

//        logger.info("[receive] {tid=" + updatePrice.getInstanceId() + "} update product, productFn " + context.self().id());
        String log_ = getPartionText(context.self().id())
                + "update price [receive], " + "tid : " + updatePrice.getInstanceId() + "\n";
        printLog(log_);

        ProductState productState = getProductState(context);
        Product product = productState.getProduct(productId);

        String result = "fail";
        if (product == null) {
            String log = getPartionText(context.self().id())
                    + "update price failed as product not exist\n"
                    + "product Id : " + productId
                    + "\n";
            logger.warning(log);
        } else {
            product.setPrice(updatePrice.getPrice());
            product.setUpdatedAt(LocalDateTime.now());
            result = "success";
            context.storage().set(PRODUCTSTATE, productState);

            String log = getPartionText(context.self().id())
                    + "update product success\n"
                    + "product Id : " + product.getProduct_id()
                    + " new price : " + product.getPrice()
                    + "\n";
//            showLog(log);
        }

        int tid = updatePrice.getInstanceId();
        long sellerId = updatePrice.getSellerId();
        // sellerID转换成string
        String response = "";
        try {
            TransactionMark transactionMark = new TransactionMark(productId, tid, String.valueOf(sellerId), result);
            ObjectMapper mapper = new ObjectMapper();
            response = mapper.writeValueAsString(transactionMark);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

//        System.out.println(getPartionText(context.self().id())+" send updatePrice response to kafka: " + response);
        context.send(
                KafkaEgressMessage.forEgress(KFK_EGRESS)
                        .withTopic("updatePriceTask")
                        .withUtf8Key(context.self().id())
                        .withUtf8Value(response)
                        .build());
//        logger.info("[success] {tid=" + updatePrice.getInstanceId() + "} update product, productFn " + context.self().id());
//        sendTaskResToSeller(context, product, Enums.TaskType.UpdatePriceType);
        String log = getPartionText(context.self().id())
                + "update price [success], " + "tid : " + updatePrice.getInstanceId() + "\n";
        printLog(log);
    }
}
