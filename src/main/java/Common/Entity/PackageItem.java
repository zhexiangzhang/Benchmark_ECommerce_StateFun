package Common.Entity;

import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Setter
@Getter
@ToString
public class PackageItem {
    @JsonProperty("packageId") private long packageId;
    @JsonProperty("orderId") private long orderId;
    @JsonProperty("shipmentId") private long shipmentId;

    // FK
    // product identification
    @JsonProperty("sellerId")  public long sellerId;
    @JsonProperty("productId") public long productId;
    @JsonProperty("freightValue") public double freightValue;
    @JsonProperty("quantity") public int quantity;
    @JsonProperty("productName") public String productName;
    @JsonProperty("packageStatus") public Enums.PackageStatus packageStatus;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("delivered_time")
    private LocalDateTime delivered_time;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("shipping_date")
    private LocalDateTime shipping_date;

    @JsonCreator
    public PackageItem(
            @JsonProperty("shipmentId") long shipmentId,
            @JsonProperty("orderId") long orderId,
            @JsonProperty("packageId") int packageId,
            @JsonProperty("sellerId") long sellerId,
            @JsonProperty("productId") long productId,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("freightValue") double freightValue,
            @JsonProperty("productName") String productName,
            @JsonProperty("shipping_date") LocalDateTime shipping_date,
            @JsonProperty("packageStatus") Enums.PackageStatus packageStatus) {
        this.packageId = packageId;
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.quantity = quantity;
        this.freightValue = freightValue;
        this.productName = productName;
        this.shipping_date = shipping_date;
        this.packageStatus = packageStatus;
    }

}
