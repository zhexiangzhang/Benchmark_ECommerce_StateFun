package Marketplace.Types.MsgToSeller;

import Common.Entity.BasketItem;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Setter
@Getter
public class IncreaseStock {
//    also send this message to stock
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<IncreaseStock> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "IncreaseStock"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, IncreaseStock.class));

    @JsonProperty("productId")
    private long productId;
    @JsonProperty("number")
    private int number;

    public IncreaseStock() {
    }

    @JsonCreator
    public IncreaseStock(@JsonProperty("productId") long productId,
                     @JsonProperty("number") int number) {
        this.productId = productId;
        this.number = number;
    }
}
