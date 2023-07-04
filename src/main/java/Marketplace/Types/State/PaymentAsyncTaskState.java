package Marketplace.Types.State;

import Common.Entity.Invoice;
import Common.Entity.Order;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class PaymentAsyncTaskState {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<PaymentAsyncTaskState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "paymentAsyncTaskState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, PaymentAsyncTaskState.class));

    @JsonProperty("TaskList")
//  how many partitions have not finish
    Map<String, Integer> TaskList = new HashMap<>();

    @JsonProperty("TmpInvoiceInfo")
    Map<String, Invoice> TmpInvoiceInfo = new HashMap<>();

    @JsonIgnore
    public void addInvoice(String uniqueOrderId, Invoice invoice) {
        TmpInvoiceInfo.put(uniqueOrderId, invoice);
    }

    @JsonIgnore
    public Invoice getInvoice(String uniqueOrderId) {
        return TmpInvoiceInfo.get(uniqueOrderId);
    }

    @JsonIgnore
    public void removeSingleInvoice(String uniqueOrderId) {
        TmpInvoiceInfo.remove(uniqueOrderId);
    }

    @JsonIgnore
    public void addNewTask(String uniqueOrderId, int taskNum) {
        TaskList.put(uniqueOrderId, taskNum);
    }

    @JsonIgnore
    public boolean isTaskComplete(String uniqueOrderId) {
        return TaskList.get(uniqueOrderId) == 0;
    }

    @JsonIgnore
    public void removeTask(String uniqueOrderId) {
        TaskList.remove(uniqueOrderId);
    }

    @JsonIgnore
    public void addCompletedSubTask(String uniqueOrderId) {
        TaskList.put(uniqueOrderId, TaskList.get(uniqueOrderId) - 1);
    }

    @JsonIgnore
    public boolean isAllSubTaskCompleted(String uniqueOrderId) {
        return TaskList.get(uniqueOrderId) == 0;
    }

}
