package Common.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderItem {
    @JsonProperty("orderId") private long orderId;
    @JsonProperty("orderItemId") private long orderItemId;
    @JsonProperty("productId") private long productId;
    @JsonProperty("productName") private String productName;
    @JsonProperty("sellerId") private long sellerId;
    @JsonProperty("unitPrice") private double unitPrice;
    @JsonProperty("freightValue") private double freightValue;
    @JsonProperty("quantity") private int quantity;
    @JsonProperty("totalPrice") private double totalPrice; // without freight
    @JsonProperty("totalAmount") private double totalAmount;
    @JsonProperty("vouchers") private double vouchers;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("shippingLimitDate") private LocalDateTime shippingLimitDate;

    public OrderItem() {
    }

    @JsonCreator
    public OrderItem(@JsonProperty("orderId") long orderId,
                     @JsonProperty("orderItemId") long orderItemId,
                     @JsonProperty("productId") long productId,
                     @JsonProperty("productName") String productName,
                     @JsonProperty("sellerId") long sellerId,
                     @JsonProperty("unitPrice") double unitPrice,
                     @JsonProperty("freightValue") double freightValue,
                     @JsonProperty("quantity") int quantity,
                     @JsonProperty("totalPrice") double totalPrice,
                     @JsonProperty("totalAmount") double totalAmount,
                     @JsonProperty("shippingLimitDate") LocalDateTime shippingLimitDate) {
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.sellerId = sellerId;
        this.unitPrice = unitPrice;
        this.freightValue = freightValue;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.totalAmount = totalAmount;
        this.shippingLimitDate = shippingLimitDate;
    }
}
