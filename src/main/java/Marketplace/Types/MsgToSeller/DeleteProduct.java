package Marketplace.Types.MsgToSeller;

import Common.Entity.Seller;
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
public class DeleteProduct {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<DeleteProduct> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "DeleteProduct"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, DeleteProduct.class));

    @JsonProperty("product_id")
    private long product_id;

    public DeleteProduct() {
    }

    @JsonCreator
    public DeleteProduct(@JsonProperty("product_id") long product_id) {
        this.product_id = product_id;
    }
}
