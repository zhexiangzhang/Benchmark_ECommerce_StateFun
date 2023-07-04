package Marketplace.Types.MsgToProdFn;

import Marketplace.Constant.Constants;
import Marketplace.Types.MsgToSeller.UpdatePrice;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.Map;

@Setter
@Getter
public class UpdateSinglePrice {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<UpdateSinglePrice> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "UpdateSinglePrice"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, UpdateSinglePrice.class));

    @JsonProperty("product_id")
    private Long product_id;
    @JsonProperty("price")
    private Double price;

    @JsonCreator
    public UpdateSinglePrice(@JsonProperty("product_id") Long product_id,
                       @JsonProperty("price") Double price) {
        this.product_id = product_id;
        this.price = price;
    }
}
