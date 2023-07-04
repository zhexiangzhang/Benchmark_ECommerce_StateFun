package Common.Entity;

import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class OrderHistory {

    @JsonProperty("id") private long id;
//    @JsonProperty("orderId") private long orderId;
    @JsonProperty("status") private Enums.OrderStatus status;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("created_at") private LocalDateTime created_at;


    @JsonCreator
    public OrderHistory() {
    }

    @JsonCreator
    public OrderHistory(@JsonProperty("id") long id,
                        @JsonProperty("created_at") LocalDateTime created_at,
                        @JsonProperty("status") Enums.OrderStatus status) {
        this.id = id;
        this.created_at = created_at;
        this.status = status;
    }
}
