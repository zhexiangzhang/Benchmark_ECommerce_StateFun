package Marketplace.Types.MsgToOrderFn;

import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Getter
@Setter
public class PaymentNotification {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<PaymentNotification> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "PaymentNotification"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, PaymentNotification.class));

    @JsonProperty("orderId")
    private long orderId;

    @JsonProperty("orderStatus")
    private Enums.OrderStatus orderStatus;

    @JsonCreator
    public PaymentNotification(@JsonProperty("orderId") long orderId,
                            @JsonProperty("orderStatus") Enums.OrderStatus orderStatus) {
        this.orderId = orderId;
        this.orderStatus = orderStatus;
    }
}
