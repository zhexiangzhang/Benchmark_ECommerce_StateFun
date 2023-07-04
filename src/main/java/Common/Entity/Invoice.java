package Common.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Invoice {
    @JsonProperty("customerCheckout")
    private CustomerCheckout customerCheckout;
    @JsonProperty("order")
    private Order order;
    @JsonProperty("items")
    private List<OrderItem> items;
    // because checkout chose a random partition to send the order
    @JsonProperty("orderPartitionID")
    private String orderPartitionID;

    public Invoice() {
    }

    @JsonCreator
    public Invoice(@JsonProperty("customerCheckout") CustomerCheckout customerCheckout,
                   @JsonProperty("order") Order order,
                   @JsonProperty("items") List<OrderItem> items,
                   @JsonProperty("orderPartitionID") String orderPartitionID) {
        this.customerCheckout = customerCheckout;
        this.order = order;
        this.items = items;
        this.orderPartitionID = orderPartitionID;
    }

}
