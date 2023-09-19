package Marketplace.Types.State;

import Common.Entity.Product;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProductState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<ProductState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "ProductState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, ProductState.class));

    @JsonProperty("products")
    private Product product;

    public ProductState() { }

    @JsonCreator
    public ProductState(@JsonProperty("products") Product product) {
        this.product = product;
    }

    @JsonIgnore
    public Product getProduct() {
        return product;
    }

    @JsonIgnore
    public void addProduct(Product product) {
        this.product = product;
    }

}
