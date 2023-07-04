package Marketplace.Types.State;

import Common.Entity.Product;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Constant.Enums.SendType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class SellerAsyncState {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<SellerAsyncState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "SellerAsyncState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, SellerAsyncState.class));

//    String : start / prodFinish / stockFinish
    @JsonProperty("addProdTask")
    Map<Long, SendType> addProdTaskList = new HashMap<>();

    @JsonProperty("deleteProdTask")
    Map<Long, SendType> deleteProdTaskList = new HashMap<>();

//    if the seller is in process of querying product => true
    @JsonProperty("queryProdTaskInProcess")
    boolean queryProdTaskInProcess = false;

//    Integer : partitionId
//    @JsonProperty("queryProdTask")
//    List<Product> productList = new ArrayList<>();

    @JsonProperty("queryProdTaskCnt")
//  how many partitions have finished querying product
    int queryProdTaskCnt = 0;

//    @JsonIgnore
//    public void addQueryProdTask(Product[] products) {
//        for (Product product : products) {
//            productList.add(product);
//        }
//    }

    @JsonIgnore
    public boolean checkQueryProdTask() {
//      if the length of productList equals to the number of partitions
        if (queryProdTaskCnt == Constants.nProductPartitions) {
            return true;
        }
        return false;
    }



//    @JsonIgnore
//    public Product[] getQueryProdTaskRes() {
//        Product[] products = new Product[productList.size()];
//        for (int i = 0; i < productList.size(); i++) {
//            products[i] = productList.get(i);
//        }
//        return products;
//    }

//    @JsonIgnore
////    clear the queryProdTask
//    public void clearQueryProdTask() {
//        productList.clear();
//    }

    @JsonIgnore
    public boolean checkAddProdTask(Long productId, Enums.SendType sendType) {
        if (addProdTaskList.containsKey(productId)) {
            SendType taskState = addProdTaskList.get(productId);
            if (taskState == SendType.ProductFn && sendType == SendType.StockFn) {
                addProdTaskList.remove(productId);
                return true;
            } else if (taskState == SendType.StockFn && sendType == SendType.ProductFn) {
                addProdTaskList.remove(productId);
                return true;
            } else {
                addProdTaskList.put(productId, sendType);
                return false;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean checkDeleteProdTask(Long productId, Enums.SendType sendType) {
        if (deleteProdTaskList.containsKey(productId)) {
            SendType taskState = deleteProdTaskList.get(productId);
            if (taskState == SendType.ProductFn && sendType == SendType.StockFn) {
                deleteProdTaskList.remove(productId);
                return true;
            } else if (taskState == SendType.StockFn && sendType == SendType.ProductFn) {
                deleteProdTaskList.remove(productId);
                return true;
            } else {
                deleteProdTaskList.put(productId, sendType);
                return false;
            }
        }
        return false;
    }
}
