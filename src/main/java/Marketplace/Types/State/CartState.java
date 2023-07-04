package Marketplace.Types.State;

import Common.Entity.BasketItem;
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
import java.util.Map;

@Getter
@Setter
public class CartState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<CartState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "CartState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, CartState.class));

    public enum Status
    {
        OPEN,
        CHECKOUT_SENT,
        PRODUCT_DIVERGENCE
    };

    @JsonProperty("status")
    public Status status;
    @JsonProperty("items")
    public Map<Long, BasketItem> items; //     private final Map<Long, BasketItem> items;

    public CartState() {
        this.status = Status.OPEN;
        this.items = new HashMap<>();
    }

//    写一个方法，接受Long和BasketItem，给items添加一个元素
    @JsonIgnore
    public void addItem(Long itemId, BasketItem item) {
//      todo 如果存在了，就用新的item覆盖原来itemid对应的值，感觉不是很合理,c#这么做
        if (this.items.containsKey(itemId)) {
            this.items.replace(itemId, item);
        }
        this.items.put(itemId, item);
    }

    @JsonIgnore
    public void removeItem(Long itemId) {
        this.items.remove(itemId);
    }

    @JsonIgnore
    public void clear() {
        this.items.clear();
    }

    @JsonIgnore
    public String getCartConent() {
    StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, BasketItem> entry : this.items.entrySet()) {
            sb.append("  " + entry.getValue().toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
