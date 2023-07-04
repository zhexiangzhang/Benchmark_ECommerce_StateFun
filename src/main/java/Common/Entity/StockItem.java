package Common.Entity;

import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockItem {
    @JsonProperty("product_id") private Long product_id;
    @JsonProperty("seller_id") private Long seller_id;
    @JsonProperty("qty_available") private int qty_available;
    @JsonProperty("qty_reserved") private int qty_reserved;
    @JsonProperty("order_count") private int order_count;
//    @JsonProperty("ytd") private int ytd;
    @JsonProperty("is_active") private Boolean is_active;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonCreator
    public StockItem(@JsonProperty("product_id") Long product_id,
                     @JsonProperty("seller_id") Long seller_id,
                     @JsonProperty("qty_available") int qty_available,
                     @JsonProperty("qty_reserved") int qty_reserved,
                     @JsonProperty("order_count") int order_count,
//                     @JsonProperty("ytd") int ytd,
                        @JsonProperty("is_active") Boolean is_active,
                        @JsonProperty("createdAt") LocalDateTime createdAt,
                        @JsonProperty("updatedAt") LocalDateTime updatedAt
        ) {
            this.product_id = product_id;
            this.seller_id = seller_id;
            this.qty_available = qty_available;
            this.qty_reserved = qty_reserved;
            this.order_count = order_count;
//            this.ytd = ytd;
            this.is_active = is_active;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
    }

//    public StockItem() {
//        this.product_id = 0;
//        this.seller_id = 0;
//        this.qty_available = 0;
//        this.qty_reserved = 0;
//        this.order_count = 0;
//        this.ytd = 0;
//        this.is_active = true;
//    }

//    @Override
//    public String toString() {
//        return "StockItem{" +
//                "product_id=" + product_id +
//                ", seller_id=" + seller_id +
//                ", qty_available=" + qty_available +
//                ", qty_reserved=" + qty_reserved +
//                ", order_count=" + order_count +
//                ", ytd=" + ytd +
//                ", is_active=" + is_active +
//                '}';
//    }
}
