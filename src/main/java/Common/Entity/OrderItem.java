package Common.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItem {
    @JsonProperty("orderId") private long orderId;
    @JsonProperty("orderItemId") private long orderItemId;
    @JsonProperty("productId") private long productId;
    @JsonProperty("sellerId") private long sellerId;
    @JsonProperty("unitPrice") private double unitPrice;
    @JsonProperty("quantity") private int quantity;
    @JsonProperty("totalPrice") private double totalPrice;

    public OrderItem() {
    }

    @JsonCreator
    public OrderItem(@JsonProperty("orderId") long orderId,
                     @JsonProperty("orderItemId") long orderItemId,
                     @JsonProperty("productId") long productId,
                     @JsonProperty("sellerId") long sellerId,
                     @JsonProperty("unitPrice") double unitPrice,
                     @JsonProperty("quantity") int quantity,
                     @JsonProperty("totalPrice") double totalPrice) {
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.sellerId = sellerId;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }
}
