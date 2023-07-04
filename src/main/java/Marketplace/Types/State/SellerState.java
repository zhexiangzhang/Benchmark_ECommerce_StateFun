package Marketplace.Types.State;

import Common.Entity.Seller;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.ArrayList;

@Setter
@Getter
public class SellerState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<SellerState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "SellerState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, SellerState.class));

    @JsonProperty("seller")
    public Seller seller;

//    存储所有属于该商家的商品ID列表ArrayList
//    @JsonProperty("productIds")
//    public ArrayList<Long> productIds;

    public SellerState() {
        this.seller = new Seller();
//        this.productIds = new ArrayList<>();
    }

//    @JsonIgnore
//    public void addProductId(Long productId) {
//        this.productIds.add(productId);
//    }
}
