package Marketplace.Types.State;

import Common.Entity.PackageItem;
import Common.Entity.Shipment;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @JsonIgnore
    public Map<Long, Long> GetOldestOpenShipmentPerSeller() {
        Map<Long, Long> q = this.packages.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.getPackageStatus().equals(Enums.PackageStatus.SHIPPED))
                .collect(Collectors.groupingBy(PackageItem::getSellerId,
                        Collectors.minBy(Comparator.comparingLong(PackageItem::getShipmentId))))
                .entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get().getShipmentId()));
        return q;
    }

    @JsonIgnore
    public List<PackageItem> GetShippedPackagesByShipmentIDAndSeller(long sellerId, long shipmentId) {
        List<PackageItem> packagesForSeller = packages.get(shipmentId).stream()
                .filter(p -> p.getSellerId() == sellerId
//                        && p.getShipmentId() == shipmentId
                        && p.getPackageStatus().equals(Enums.PackageStatus.SHIPPED))
                .collect(Collectors.toList());
        return packagesForSeller;
    }

    @JsonIgnore
    public int GetTotalDeliveredPackagesForShipment(long shipmentId) {
        int countDelivered = (int) packages.get(shipmentId).stream()
                .filter(p -> p.getPackageStatus() == Enums.PackageStatus.DELIVERED)
                .count();
        return countDelivered;
    }
}
