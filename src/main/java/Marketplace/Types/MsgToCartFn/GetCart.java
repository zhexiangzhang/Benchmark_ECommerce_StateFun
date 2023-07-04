package Marketplace.Types.MsgToCartFn;

import Marketplace.Constant.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Getter
@Setter
public class GetCart {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<GetCart> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "GetCart"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, GetCart.class));

    GetCart() {
    }
}
