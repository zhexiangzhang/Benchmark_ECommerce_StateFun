package Marketplace.Types;

import Marketplace.Constant.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;


@Getter
@Setter
public class ClearState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<ClearState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "ClearState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, ClearState.class));

    ClearState() {
    }
}
