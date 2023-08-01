package Marketplace.Types.State;

import Common.Entity.Order;
import Common.Entity.OrderHistory;
import Common.Entity.OrderItem;
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

import java.util.*;

@Setter
@Getter
public class OrderState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<OrderState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "OrderState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, OrderState.class));

    @JsonProperty("orders")
    public Map<Long, Order> orders = new HashMap<>();

    // 集合
    @JsonProperty("orderItems")
    public Set<OrderItem> orderItems = new HashSet<>();

    @JsonProperty("orderHistory")
    public TreeMap<Long, List<OrderHistory>> orderHistory = new TreeMap<>();

    @JsonProperty("customerOrderID")
    public Map<Long, Long> customerOrderID = new HashMap<>();

    @JsonIgnore
    public void addOrder(long orderId, Order order) {
        orders.put(orderId, order);
    }

    @JsonIgnore
    public long generateCustomerNextOrderID(long customerId) {
        if (!customerOrderID.containsKey(customerId)) {
            customerOrderID.put(customerId, 1L);
            return 1L;
        } else {
            long newId = customerOrderID.get(customerId) + 1;
            customerOrderID.put(customerId, newId);
            return newId;
        }
    }

    @JsonIgnore
    public void addOrderHistory(long orderId, OrderHistory orderHistory) {
        if (this.orderHistory.containsKey(orderId)) {
            this.orderHistory.get(orderId).add(orderHistory);
        } else {
            List<OrderHistory> historyList = new ArrayList<>();
            historyList.add(orderHistory);
            this.orderHistory.put(orderId, historyList);
        }
    }

    @JsonIgnore
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
    }

    @JsonCreator
    public OrderState() {
    }
}
