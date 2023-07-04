package Common.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class CustomerCheckout {

    @JsonProperty("customerId") private long customerId;
    @JsonProperty("name") private String name;
    @JsonProperty("address") private String address;
    @JsonProperty("zipCode") private String zipCode;
    @JsonProperty("cardNumber") private String cardNumber;
    @JsonProperty("cardSecurityNumber") private String cardSecurityNumber;
    @JsonProperty("cardExpiration") private String cardExpiration;
}
