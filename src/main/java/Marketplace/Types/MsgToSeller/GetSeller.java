package Marketplace.Types.MsgToSeller;

import Marketplace.Constant.Constants;
import Marketplace.Types.MsgToCartFn.GetCart;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

public class GetSeller {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<GetSeller> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "GetSeller"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, GetSeller.class));

    GetSeller() {
    }
}
