package Marketplace.Types.State;

import Common.Entity.Product;
import Common.Entity.Seller;
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
    public HashMap<Long, Product> products;

    public ProductState() {
        products = new HashMap<>();
    }

    @JsonCreator
    public ProductState(@JsonProperty("products") HashMap<Long, Product> products) {
        this.products = products;
    }

    @JsonIgnore
    public Product getProduct(Long product_id) {
        return products.get(product_id);
    }

    @JsonIgnore
    public void addProduct(Product product) {
        products.put(product.getId(), product);
    }

//    @JsonIgnore
//    public void deleteProduct(Long product_id) {
//        products.remove(product_id);
//    }

    @JsonIgnore
//    find all products of a seller
    public Product[] getProductsOfSeller(Long seller_id) {
        List<Product> productsOfSeller = products.values().stream()
                .filter(product -> product.getSellerId() == seller_id)
                .collect(Collectors.toList());

        return productsOfSeller.toArray(new Product[productsOfSeller.size()]);

    }
}
