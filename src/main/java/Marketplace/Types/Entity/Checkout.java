package Marketplace.Types.Entity;
import Common.Entity.BasketItem;
import Common.Entity.CustomerCheckout;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class Checkout {
    @JsonProperty("createdAt")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;
    @JsonProperty("customerCheckout")
    private CustomerCheckout customerCheckout;
    @JsonProperty("items")
    private Map<Long, BasketItem> items;

//    public Checkout() {
//    }
//    public Checkout(LocalDateTime now, CustomerCheckout customerCheckout, Map<Long, BasketItem> items) {
//        this.createdAt = now;
//        this.customerCheckout = customerCheckout;
//        this.items = items;
//    }

//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public CustomerCheckout getCustomerCheckout() {
//        return customerCheckout;
//    }
//
//    public void setCustomerCheckout(CustomerCheckout customerCheckout) {
//        this.customerCheckout = customerCheckout;
//    }
//
//    public Map<Long, BasketItem> getItems() {
//        return items;
//    }
//
//    public void setItems(Map<Long, BasketItem> items) {
//        this.items = items;
//    }
}
