package Marketplace.Types.State;

import Common.Entity.StockItem;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.time.LocalDateTime;
import java.util.HashMap;

@Setter
@Getter
public class StockState {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<StockState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "StockState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, StockState.class));

    @JsonProperty("stockItems")
    public HashMap<Long, StockItem> stockItems;

    public StockState() {
        stockItems = new HashMap<>();
    }

    @JsonCreator
    public StockState(@JsonProperty("stockItems") HashMap<Long, StockItem> stockItems) {
        this.stockItems = stockItems;
    }

    @JsonIgnore
    public StockItem getItem(Long product_id) {
        return stockItems.get(product_id);
    }

    @JsonIgnore
    public void increaseStock(Long product_id, int num) {
        StockItem stockItem = this.getItem(product_id);
        stockItem.setQty_available(stockItem.getQty_available() + num);
        stockItem.setUpdatedAt(LocalDateTime.now());
    }

    @JsonIgnore
    public void addItem(StockItem stockItem) {
        stockItems.put(stockItem.getProduct_id(), stockItem);
    }
}
