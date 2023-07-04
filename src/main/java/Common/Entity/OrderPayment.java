package Common.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderPayment {
    @JsonProperty("orderId") private String orderId;
    @JsonProperty("cardHolderName") private String cardHolderName;
    @JsonProperty("cardNumber") private String cardNumber;
    @JsonProperty("cardExpiration") private String cardExpiration;

    @JsonCreator
    public OrderPayment(@JsonProperty("orderId") String orderId,
                        @JsonProperty("cardHolderName") String cardHolderName,
                        @JsonProperty("cardNumber") String cardNumber,
                        @JsonProperty("cardExpiration") String cardExpiration) {
        this.orderId = orderId;
        this.cardHolderName = cardHolderName;
        this.cardNumber = cardNumber;
        this.cardExpiration = cardExpiration;
    }
}
