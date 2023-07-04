package Marketplace.Types.State;

import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToStock.CheckoutResv;
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
public class OrderAsyncTaskState {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<OrderAsyncTaskState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "OrderAsyncState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, OrderAsyncTaskState.class));

    // long: customerId  long : productId
    @JsonProperty("attemptResTaskList")
//    Map<Long, Map<Long, Enums.ItemStatus>> attemptResTaskList = new HashMap<>();
    Map<Long, List<CheckoutResv>> attemptResTaskList = new HashMap<>();

    @JsonProperty("attemptResTaskCntList")
//  how many partitions have not finishe attempting reservation
    Map<Long, Integer> attemptResTaskCntList = new HashMap<>();

    // confirm reservation or cancel reservation
    @JsonProperty("decisionTaskCntList")
    Map<Long, Integer> decisionTaskCntList = new HashMap<>();

    @JsonProperty("resultTaskTypeList")
    Map<Long, Enums.TaskType> decisionTaskTypeList = new HashMap<>();

    @JsonIgnore
    public void addNewTask(long customerId, int taskNum, Enums.TaskType taskType) {
        if(taskType == Enums.TaskType.AttemptReservationsType) {
            attemptResTaskCntList.put(customerId, taskNum);
            attemptResTaskList.put(customerId, new ArrayList<>());
        } else {
            decisionTaskCntList.put(customerId, taskNum);
            decisionTaskTypeList.put(customerId, taskType);
        }
    }

    @JsonIgnore
    public List<CheckoutResv> getSuccessAttempResvSubtask(long customerId) {
        List<CheckoutResv> checkoutResvList = attemptResTaskList.get(customerId);
        List<CheckoutResv> successCheckoutResvList = new ArrayList<>();
        for(CheckoutResv checkoutResv : checkoutResvList) {
            if(checkoutResv.getItemStatus() == Enums.ItemStatus.IN_STOCK) {
                successCheckoutResvList.add(checkoutResv);
            }
        }
        return successCheckoutResvList;
    }

    @JsonIgnore
    public List<CheckoutResv> getSingleCheckoutResvTask(long customerId) {
        return attemptResTaskList.get(customerId);
    }

    @JsonIgnore
    public void addCompletedSubTask(long customerId, CheckoutResv checkoutResv, Enums.TaskType taskType) {
        if(taskType == Enums.TaskType.AttemptReservationsType) {
            // Decrement the taskNum for the customerId corresponding to attemptResTaskCntList.
            int taskNum = attemptResTaskCntList.get(customerId);
            taskNum = taskNum - 1;
            attemptResTaskCntList.put(customerId, taskNum);
            // add the checkoutResv to the attemptResTaskList
            List<CheckoutResv> checkoutResvList = attemptResTaskList.get(customerId);
            checkoutResvList.add(checkoutResv);
            attemptResTaskList.put(customerId, checkoutResvList);
        } else {
            // Decrement the taskNum for the customerId corresponding to attemptResTaskCntList.
            int taskNum = decisionTaskCntList.get(customerId);
            taskNum = taskNum - 1;
            decisionTaskCntList.put(customerId, taskNum);
        }
    }

    @JsonIgnore
    public boolean isTaskComplete(long customerId, Enums.TaskType taskType) {
        if(taskType == Enums.TaskType.AttemptReservationsType) {
            if (attemptResTaskCntList.get(customerId) == 0) {
                return true;
            }
            return false;
        } else {
            if (decisionTaskCntList.get(customerId) == 0) {
                return true;
            }
            return false;
        }
    }

    @JsonIgnore
    public boolean isTaskSuccess(long customerId) {
        List<CheckoutResv> checkoutResvList = attemptResTaskList.get(customerId);
        for (CheckoutResv checkoutResv : checkoutResvList) {
            if (checkoutResv.getItemStatus() != Enums.ItemStatus.IN_STOCK) {
                return false;
            }
        }
        return true;
    }

    @JsonIgnore
    public void removeTask(long customerId, Enums.TaskType taskType) {
        if (taskType == Enums.TaskType.AttemptReservationsType) {
            attemptResTaskCntList.remove(customerId);
            attemptResTaskList.remove(customerId);
        } else {
            decisionTaskCntList.remove(customerId);
            decisionTaskTypeList.remove(customerId);
        }
    }
}
