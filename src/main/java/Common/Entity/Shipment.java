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

import java.time.LocalDateTime;


@Getter
@Setter
public class Shipment {
    @JsonProperty("shipmentId") private long shipmentId;
    @JsonProperty("orderId") private long orderId;
    @JsonProperty("orderPartition") private long orderPartition;
    @JsonProperty("customerId") private long customerId;
    @JsonProperty("name") private String name;
    @JsonProperty("packageCnt") private int packageCnt;
    @JsonProperty("address") private String address;
    @JsonProperty("zipCode") private String zipCode;
//    @JsonProperty("packageStatus") private Enums.PackageStatus packageStatus;
    @JsonProperty("packageStatus") String packageStatus;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("requestDate") private LocalDateTime requestDate;

    @JsonCreator
    public Shipment(
            @JsonProperty("shipmentId") long shipmentId,
            @JsonProperty("orderId") long orderId,
            @JsonProperty("orderPartition") long orderPartition,
            @JsonProperty("customerId") long customerId,
            @JsonProperty("name") String name,
            @JsonProperty("packageCnt") int packageCnt,
            @JsonProperty("address") String address,
            @JsonProperty("zipCode") String zipCode,
            @JsonProperty("requestDate") LocalDateTime requestDate,
//            @JsonProperty("packageStatus") Enums.PackageStatus packageStatus
            @JsonProperty("packageStatus") String packageStatus
    ) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.orderPartition = orderPartition;
        this.customerId = customerId;
        this.name = name;
        this.packageCnt = packageCnt;
        this.address = address;
        this.zipCode = zipCode;
        this.requestDate = requestDate;
        this.packageStatus = packageStatus;
    }
}
