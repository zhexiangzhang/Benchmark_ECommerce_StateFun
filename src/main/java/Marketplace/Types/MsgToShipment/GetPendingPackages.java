package Marketplace.Types.MsgToShipment;

import Common.Entity.Invoice;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Getter
@Setter
public class GetPendingPackages {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<GetPendingPackages> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "GetPendingPackages"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, GetPendingPackages.class));

    @JsonProperty("sellerID")
    private long sellerID;

    @JsonCreator
    public GetPendingPackages(@JsonProperty("sellerID") long sellerID) {
        this.sellerID = sellerID;
    }
}
