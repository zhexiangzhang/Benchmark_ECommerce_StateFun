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
    @JsonProperty("customerId") private long customerId;
    @JsonProperty("packageCnt") private int packageCnt;
    @JsonProperty("totalFreight") private double totalFreight;
    @JsonProperty("orderPartition") private long orderPartition;

    @JsonProperty("firstName") private String firstName;
    @JsonProperty("lastName") private String lastName;
    @JsonProperty("street") private String street;
    @JsonProperty("zipCode") private String zipCode;
    @JsonProperty("status") Enums.ShipmentStatus status;
    @JsonProperty("city") private String city;
    @JsonProperty("state") private String state;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("requestDate") private LocalDateTime requestDate;

    @JsonCreator
    public Shipment(
            @JsonProperty("shipmentId") long shipmentId,
            @JsonProperty("orderId") long orderId,
            @JsonProperty("customerId") long customerId,
            @JsonProperty("packageCnt") int packageCnt,

            @JsonProperty("totalFreight") double totalFreight,
            @JsonProperty("requestDate") LocalDateTime requestDate,
            @JsonProperty("status") Enums.ShipmentStatus status,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("street") String street,

//            @JsonProperty("orderId") long orderId,
            @JsonProperty("orderPartition") long orderPartition,

            @JsonProperty("zipCode") String zipCode,
            @JsonProperty("city") String city,
            @JsonProperty("state") String state
    ) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.packageCnt = packageCnt;
        this.orderPartition = orderPartition;
        this.totalFreight = totalFreight;
        this.customerId = customerId;
        this.status = status;
        this.firstName = firstName;
        this.lastName = lastName;
        this.street = street;
        this.requestDate = requestDate;
        this.zipCode = zipCode;
        this.city = city;
        this.state = state;
    }
}
