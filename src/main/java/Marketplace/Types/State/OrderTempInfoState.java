package Marketplace.Types.State;

import Marketplace.Constant.Constants;
import Common.Entity.Checkout;
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

//checkout info
@Getter
@Setter
public class OrderTempInfoState {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<OrderTempInfoState> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "OrderTempInfoState"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, OrderTempInfoState.class));

    @JsonProperty("checkout")
    private Map<Long, Checkout> checkout = new HashMap<>();

    @JsonIgnore
    public void addCheckout(Long customerId, Checkout checkout) {
        this.checkout.put(customerId, checkout);
    }

    @JsonIgnore
    public void removeSingleCheckout(Long customerId) {
        this.checkout.remove(customerId);
    }

    @JsonIgnore
    public Checkout getSingleCheckout(Long customerId) {
        return this.checkout.get(customerId);
    }
}
