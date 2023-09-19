package Marketplace.Types.MsgToStock;

import Common.Entity.BasketItem;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Setter
@Getter
public class ReserveStockEvent {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<ReserveStockEvent> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "CheckoutResv"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, ReserveStockEvent.class));

    @JsonProperty("customerId")
    int customerId;

    @JsonProperty("item")
    BasketItem item;

    @JsonProperty("ItemStatus")
    private Enums.ItemStatus ItemStatus;

    @JsonCreator
    public ReserveStockEvent(@JsonProperty("customerId") int customerId,
                             @JsonProperty("item") BasketItem item,
                             @JsonProperty("ItemStatus") Enums.ItemStatus ItemStatus
    ) {
        this.customerId = customerId;
        this.item = item;
        this.ItemStatus = ItemStatus;
    }
}
