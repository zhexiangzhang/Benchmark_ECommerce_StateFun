package Marketplace.Types.MsgToSeller;

import Common.Entity.CustomerCheckout;
import Common.Entity.Seller;
import Marketplace.Constant.Constants;
import Marketplace.Types.MsgToCartFn.GetCart;
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
public class InitSeller {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<InitSeller> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "InitSeller"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, InitSeller.class));

    @JsonProperty("seller")
    private Seller seller;

    public InitSeller() {
    }

    @JsonCreator
    public InitSeller(@JsonProperty("seller") Seller seller) {
        this.seller = seller;
    }
}
