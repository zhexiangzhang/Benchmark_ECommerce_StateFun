package Marketplace.Types.MsgToCartFn;

import Common.Entity.BasketItem;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;


@Getter
@Setter
public class AddToCart {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<AddToCart> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "AddToCart"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, AddToCart.class));

    @JsonProperty("customerId")
    private Long customerId;

    @JsonProperty("item")
    private BasketItem item;

    public AddToCart() {
    }

    @JsonCreator
    public AddToCart(@JsonProperty("customerId") Long customerId,
                     @JsonProperty("item") BasketItem item) {
        this.customerId = customerId;
        this.item = item;
    }
}
