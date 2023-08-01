package Marketplace.Types.State;

import Common.Entity.OrderEntry;
import Common.Entity.OrderEntryDetails;
import Common.Entity.Seller;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
public class SellerState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<SellerState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "SellerState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, SellerState.class));

    @JsonProperty("seller")
    public Seller seller;

    @JsonProperty("orderEntriesHistory")
//    public Map<Long, OrderEntry> orderEntriesHistory;
    public Set<OrderEntry> orderEntriesHistory;

    // entry in process, only for INVOICED / PAYMENT_PORCESSED / READY_FOR_SHIPMENT / IN_TRANSIT
    @JsonProperty("orderEntries")
//    public Map<Long, OrderEntry> orderEntries;
    public Set<OrderEntry> orderEntries;

    // DELIVERED
    @JsonProperty("OrderEntryDetails")
    public Map<Long, OrderEntryDetails> orderEntryDetails;
//    public Set<OrderEntryDetails> orderEntryDetails;

    @JsonCreator
    public SellerState() {
        this.seller = new Seller();
        this.orderEntriesHistory = new HashSet<>();
        this.orderEntries = new HashSet<>();
        this.orderEntryDetails = new java.util.HashMap<>();
    }

    @JsonIgnore
    public void addOrderEntry(OrderEntry orderEntry) {
        this.orderEntries.add(orderEntry);
    }

    @JsonIgnore
    public void addOrderEntryDetails(long orderId, OrderEntryDetails orderEntryDetails) {
        this.orderEntryDetails.put(orderId, orderEntryDetails);
    }

    @JsonIgnore
    public void moveOrderEntryToHistory(long orderEntryId) {
        OrderEntry orderEntry = this.orderEntries.stream().filter(o -> o.getOrder_id() == orderEntryId).findFirst().get();
        this.orderEntries.remove(orderEntry);
        this.orderEntriesHistory.add(orderEntry);
    }

    @JsonIgnore
    public void updateOrderStatus(long orderEntryId, Enums.OrderStatus orderStatus, LocalDateTime updateTime) {
        // 更新orderEntries,如果更新后的不属于only for INVOICED / PAYMENT_PORCESSED / READY_FOR_SHIPMENT / IN_TRANSIT，
        // 则将其移动到orderEntriesHistory
        for (OrderEntry orderEntry : this.orderEntries) {
            if (orderEntry.getOrder_id() == orderEntryId) {
                this.orderEntries.remove(orderEntry);
                orderEntry.setOrder_status(orderStatus);

                if (orderStatus == Enums.OrderStatus.IN_TRANSIT) {
                    orderEntry.setShipment_date(updateTime);
                }

                if (orderStatus == Enums.OrderStatus.INVOICED
                        || orderStatus == Enums.OrderStatus.PAYMENT_PROCESSED
                        || orderStatus == Enums.OrderStatus.READY_FOR_SHIPMENT
                        || orderStatus == Enums.OrderStatus.IN_TRANSIT)
                {
                    this.orderEntries.add(orderEntry);
                }
                else
                {
                    this.orderEntriesHistory.add(orderEntry);
                }
                break;
            }
        }

        // 更新orderEntryDetails
        for (OrderEntryDetails orderEntryDetails : this.orderEntryDetails.values()) {
            if (orderEntryDetails.getOrderId() == orderEntryId) {
                orderEntryDetails.setOrder_status(orderStatus);
                break;
            }
        }
    }


//    @JsonIgnore
//    public void addProductId(Long productId) {
//        this.productIds.add(productId);
//    }
}
