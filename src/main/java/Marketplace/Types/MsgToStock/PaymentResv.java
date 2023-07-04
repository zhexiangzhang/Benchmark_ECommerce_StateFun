package Marketplace.Types.MsgToStock;

import Common.Entity.BasketItem;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Setter
@Getter
@ToString
public class PaymentResv {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<PaymentResv> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "PaymentResv"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, PaymentResv.class));

    @JsonProperty("productId")
    long productId;

    @JsonProperty("quantity")
    int quantity;

    @JsonProperty("uniqueOrderId")
    String uniqueOrderId;

    @JsonProperty("OrderStatus")
    private Enums.OrderStatus OrderStatus;

    @JsonCreator
    public PaymentResv(@JsonProperty("uniqueOrderId") String uniqueOrderId,
                       @JsonProperty("productId") long productId,
                       @JsonProperty("quantity") int quantity,
                       @JsonProperty("OrderStatus") Enums.OrderStatus OrderStatus

    ) {
        this.uniqueOrderId = uniqueOrderId;
        this.productId = productId;
        this.quantity = quantity;
        this.OrderStatus = OrderStatus;
    }
}
