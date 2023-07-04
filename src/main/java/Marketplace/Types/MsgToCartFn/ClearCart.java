package Marketplace.Types.MsgToCartFn;

import Marketplace.Constant.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

public class ClearCart {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<ClearCart> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "ClearCart"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, ClearCart.class));

    ClearCart() {
    }
}
