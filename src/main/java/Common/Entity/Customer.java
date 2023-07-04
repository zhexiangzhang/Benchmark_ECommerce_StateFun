package Common.Entity;

import Marketplace.Types.MsgToSeller.IncreaseStock;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class Customer {

    @JsonProperty("customerId") private long customerId;

    @JsonProperty("name") private String name;
    @JsonProperty("address") private String address;

    // olist data set
    @JsonProperty("zipCode") private String zipCode;

    // card
    @JsonProperty("cardNumber") private String cardNumber;
    @JsonProperty("cardSecurityNumber") private String cardSecurityNumber;
    @JsonProperty("cardExpiration") private String cardExpiration;

    // statistics
    @JsonProperty("successPaymentCount") private int successPaymentCount;
    @JsonProperty("failedPaymentCount") private int failedPaymentCount;
    @JsonProperty("pendingDeliveriesCount") private int pendingDeliveriesCount;
    @JsonProperty("deliveryCount") private int deliveryCount;
    @JsonProperty("abandonedCartCount") private int abandonedCartCount;

    @JsonProperty("totalSpentItems") private BigDecimal totalSpentItems;
    @JsonProperty("totalSpentFreights") private BigDecimal totalSpentFreights;

    @JsonCreator
    public Customer(
            @JsonProperty("customerId") long customerId,
            @JsonProperty("name") String name,
            @JsonProperty("address") String address,
            @JsonProperty("zipCode") String zipCode,
            @JsonProperty("cardNumber") String cardNumber,
            @JsonProperty("cardSecurityNumber") String cardSecurityNumber,
            @JsonProperty("cardExpiration") String cardExpiration) {
        this.customerId = customerId;
        this.name = name;
        this.address = address;
        this.zipCode = zipCode;
        this.cardNumber = cardNumber;
        this.cardSecurityNumber = cardSecurityNumber;
        this.cardExpiration = cardExpiration;
        this.successPaymentCount = 0;
        this.failedPaymentCount = 0;
        this.pendingDeliveriesCount = 0;
        this.deliveryCount = 0;
        this.abandonedCartCount = 0;
        this.totalSpentItems = BigDecimal.ZERO;
        this.totalSpentFreights = BigDecimal.ZERO;
    }
}
