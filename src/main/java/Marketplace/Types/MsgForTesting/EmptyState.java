package Marketplace.Types.MsgForTesting;

import Marketplace.Constant.Constants;
import Marketplace.Types.MsgToStock.CheckoutResv;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

public class EmptyState {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<EmptyState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "EmptyState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, EmptyState.class));
}
