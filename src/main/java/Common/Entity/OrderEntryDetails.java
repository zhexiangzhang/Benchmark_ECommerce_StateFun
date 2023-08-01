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

@Setter
@Getter
public class OrderEntryDetails {
    // primary key
    @JsonProperty("OrderId") private long orderId;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("order_date")
    private LocalDateTime order_date;

    @JsonProperty("order_status") private Enums.OrderStatus order_status;

    @JsonProperty("CustomerId") private long customerId;
    @JsonProperty("FirstName") private String firstName;
    @JsonProperty("LastName") private String lastName;
    @JsonProperty("Street") private String street;
    @JsonProperty("Complement") private String complement;
    @JsonProperty("City") private String city;
    @JsonProperty("State") private String state;
    @JsonProperty("ZipCode") private String zipCode;

    @JsonProperty("CardBrand") private String cardBrand;
    @JsonProperty("Installments") private int installments;

    @JsonCreator
    public OrderEntryDetails(
            @JsonProperty("OrderId") long orderId,
            @JsonProperty("order_date") LocalDateTime order_date,
            @JsonProperty("order_status") Enums.OrderStatus order_status,
            @JsonProperty("CustomerId") long customerId,
            @JsonProperty("FirstName") String firstName,
            @JsonProperty("LastName") String lastName,
            @JsonProperty("Street") String street,
            @JsonProperty("Complement") String complement,
            @JsonProperty("City") String city,
            @JsonProperty("State") String state,
            @JsonProperty("ZipCode") String zipCode,
            @JsonProperty("CardBrand") String cardBrand,
            @JsonProperty("Installments") int installments
    ) {
        this.orderId = orderId;
        this.order_date = order_date;
        this.order_status = order_status;
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.street = street;
        this.complement = complement;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.cardBrand = cardBrand;
        this.installments = installments;
    }
}
