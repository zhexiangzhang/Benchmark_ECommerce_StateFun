package Marketplace.Types.State;

import Common.Entity.BasketItem;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToStock.ReserveStockEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.*;

@Setter
@Getter
public class ReserveStockTaskState {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<ReserveStockTaskState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "OrderAsyncState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, ReserveStockTaskState.class));

    // long: customerId  long : productId
//    @JsonProperty("attemptResTaskList")
////    Map<Long, Map<Long, Enums.ItemStatus>> attemptResTaskList = new HashMap<>();
//    Map<Long, List<CheckoutResv>> attemptResTaskList = new HashMap<>();

    @JsonProperty("remainSubTaskCnt")
//  how many partitions have not finishe attempting reservation
    Map<Long, Integer> remainSubTaskCnt = new HashMap<>();

    @JsonProperty("itemsSuccessResv")
    Map<Long, List<BasketItem>> itemsSuccessResv = new HashMap<>();
    @JsonProperty("itemsFailedResv")
    Map<Long, List<BasketItem>> itemsFailedResv = new HashMap<>();

    @JsonIgnore
    public void addNewTask(long customerId, int taskNum) {
        remainSubTaskCnt.put(customerId, taskNum);
//        attemptResTaskList.put(customerId, new ArrayList<>());
        itemsSuccessResv.put(customerId, new ArrayList<>());
        itemsFailedResv.put(customerId, new ArrayList<>());
    }

//    @JsonIgnore
//    public List<CheckoutResv> getSuccessAttempResvSubtask(long customerId) {
//        List<CheckoutResv> checkoutResvList = attemptResTaskList.get(customerId);
//        List<CheckoutResv> successCheckoutResvList = new ArrayList<>();
//        for(CheckoutResv checkoutResv : checkoutResvList) {
//            if(checkoutResv.getItemStatus() == Enums.ItemStatus.IN_STOCK) {
//                successCheckoutResvList.add(checkoutResv);
//            }
//        }
//        return successCheckoutResvList;
//    }

//    @JsonIgnore
//    public List<CheckoutResv> getSingleResvTask(long customerId) {
//        return attemptResTaskList.get(customerId);
//    }

    @JsonIgnore
    public Map<Long, BasketItem> getSingleSuccessResvItems(long customerId) {
        List<BasketItem> items = itemsSuccessResv.get(customerId);
        Map<Long, BasketItem> itemsMap = new HashMap<>();
        for(BasketItem item : items) {
            itemsMap.put(item.getProductId(), item);
        }
        return itemsMap;
    }

    @JsonIgnore
    public Map<Long, BasketItem> getSingleFailedResvItems(long customerId) {
        List<BasketItem> items = itemsFailedResv.get(customerId);
        Map<Long, BasketItem> itemsMap = new HashMap<>();
        for(BasketItem item : items) {
            itemsMap.put(item.getProductId(), item);
        }
        return itemsMap;
    }

    @JsonIgnore
    public void addCompletedSubTask(long customerId, ReserveStockEvent itemRes) {
        // Decrement the taskNum for the customerId corresponding to attemptResTaskCntList.
        int taskNum = remainSubTaskCnt.get(customerId);
        taskNum = taskNum - 1;
        remainSubTaskCnt.put(customerId, taskNum);
        // add the checkoutResv to two list
        List<BasketItem> itemsSuccessResv_ = this.itemsSuccessResv.get(customerId);
        List<BasketItem> itemsFailedResv_ = this.itemsFailedResv.get(customerId);
        if (itemRes.getItemStatus() == Enums.ItemStatus.IN_STOCK) {
            itemsSuccessResv_.add(itemRes.getItem());
        } else {
            itemsFailedResv_.add(itemRes.getItem());
        }

        this.itemsSuccessResv.put(customerId, itemsSuccessResv_);
        this.itemsFailedResv.put(customerId, itemsFailedResv_);
    }

    @JsonIgnore
    public boolean isTaskComplete(long customerId) {
        if (remainSubTaskCnt.get(customerId) == 0) {
            return true;
        }
        return false;
    }

    @JsonIgnore
    public void removeTask(long customerId) {
        remainSubTaskCnt.remove(customerId);
//        attemptResTaskList.remove(customerId);
        itemsSuccessResv.remove(customerId);
        itemsFailedResv.remove(customerId);
    }
}
