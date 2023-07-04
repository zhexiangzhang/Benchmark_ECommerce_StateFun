package Marketplace.Types.State;

import Common.Entity.PackageItem;
import Common.Entity.Shipment;
import Marketplace.Constant.Constants;
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
import java.util.Map;

@Setter
@Getter
public class ShipmentState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<ShipmentState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "ShipmentState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, ShipmentState.class));

    @JsonProperty("shipments") private Map<Long, Shipment> shipments = new HashMap<>();
    @JsonProperty("packages") private Map<Long, List<PackageItem>> packages = new HashMap<>();

    @JsonIgnore
    public void addShipment(long orderId, Shipment shipment) {
        shipments.put(orderId, shipment);
    }

    @JsonIgnore
    public void addPackage(long orderId, List<PackageItem> packageItem) {
        packages.put(orderId, packageItem);
    }
}
