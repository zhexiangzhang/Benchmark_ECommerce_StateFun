package Marketplace.Types.MsgToProdFn;

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
public class UpdateSinglePrice {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<UpdateSinglePrice> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "UpdateSinglePrice"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, UpdateSinglePrice.class));

    @JsonProperty("sellerId")
    private Long sellerId;
    @JsonProperty("productId")
    private Long productId;
    @JsonProperty("price")
    private Double price;
    @JsonProperty("instanceId")
    private int instanceId;

    @JsonCreator
    public UpdateSinglePrice(@JsonProperty("sellerId") Long sellerId,
                             @JsonProperty("productId") Long productId,
                             @JsonProperty("price") Double price,
                             @JsonProperty("instanceId") int instanceId) {
        this.sellerId = sellerId;
        this.productId = productId;
        this.price = price;
        this.instanceId = instanceId;
    }
}
