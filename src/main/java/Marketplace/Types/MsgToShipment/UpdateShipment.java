package Marketplace.Types.MsgToShipment;

import Marketplace.Constant.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

public class UpdateShipment {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<UpdateShipment> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "UpdateShipment"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, UpdateShipment.class));
}
