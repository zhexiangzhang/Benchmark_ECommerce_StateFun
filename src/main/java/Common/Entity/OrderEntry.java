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

import java.time.LocalDateTime;

// for dashboard

@Getter
@Setter
@ToString
public class OrderEntry {
    @JsonProperty("seller_id") private long seller_id;
    @JsonProperty("order_id") private long order_id;
    @JsonProperty("package_id") private long package_id;

    @JsonProperty("product_id") private long product_id;
    @JsonProperty("product_name") private String product_name = "";
    @JsonProperty("product_category") private String product_category = "";

    @JsonProperty("unit_price") private double unit_price;
    @JsonProperty("quantity") private int quantity;

    @JsonProperty("totalItems") private double totalItems;
    @JsonProperty("totalAmount") private double totalAmount;
    @JsonProperty("totalInvoice") private double totalInvoice;
    @JsonProperty("totalIncentive") private double totalIncentive = 0;
    @JsonProperty("freight_value") private double freight_value;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("shipment_date")
    private LocalDateTime shipment_date;
//
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonProperty("delivery_date")
    private LocalDateTime delivery_date;

    // denormalized, thus redundant. to avoid join on details
    // 这个字段的设计虽然在数据上是冗余的，（同样的信息在数据库中可能被存储了多次），这是为了避免在查询订单状态时需要进行联接查询（join），从而提高查询性能。这种设计策略被称为“反规范化”（denormalized）。
    // 这个两个字段是冗余的，我们来想把他们放到单独的表，但是R建议不要这样，只是两个field，增加存储没事。
    // 如果不这样，每次要查询状态再join，会慢且与Dapr的设计不符
    @JsonProperty("order_status") private Enums.OrderStatus order_status;
    @JsonProperty("delivery_status") private Enums.PackageStatus delivery_status;

    // 对于 OrderEntryDetails 这个字段，所有的order共享一个，作为外键，只有在查询时数据才会被填充，所以我们也把他放在额外的表
    // from down below, all the same. could be normalized.... e.g., order_details table, shared across sellers
    //  [ForeignKey("order_id")]
    @JsonProperty("orderEntryDetails")
    public OrderEntryDetails orderEntryDetails = null;

    @JsonCreator
    public OrderEntry() {
    }

    @JsonCreator
    public OrderEntry(
            @JsonProperty("order_id") long order_id,
            @JsonProperty("seller_id") long seller_id,
//            @JsonProperty("package_id") long package_id,
            @JsonProperty("product_id") long product_id,
            @JsonProperty("product_name") String product_name,

            @JsonProperty("quantity") int quantity,
            @JsonProperty("totalAmount") double totalAmount,
            @JsonProperty("totalItems") double totalItems,

//            @JsonProperty("totalInvoice") double totalInvoice,
//            @JsonProperty("totalIncentive") double totalIncentive,

            @JsonProperty("freight_value") double freight_value,

//            @JsonProperty("shipment_date") LocalDateTime shipment_date,
//            @JsonProperty("delivery_date") LocalDateTime delivery_date,
            @JsonProperty("unit_price") double unit_price,
            @JsonProperty("order_status") Enums.OrderStatus order_status


//            @JsonProperty("product_category") String product_category,

//            @JsonProperty("delivery_status") Enums.ShipmentStatus delivery_status
    ) {
        this.seller_id = seller_id;
        this.order_id = order_id;
//        this.package_id = package_id;
        this.product_id = product_id;
        this.product_name = product_name;
//        this.product_category = product_category;
        this.unit_price = unit_price;
        this.quantity = quantity;
        this.totalItems = totalItems;
        this.totalAmount = totalAmount;
//        this.totalInvoice = totalInvoice;
        this.totalIncentive = totalIncentive;
        this.freight_value = freight_value;
//        this.shipment_date = shipment_date;
//        this.delivery_date = delivery_date;
        this.order_status = order_status;
//        this.delivery_status = delivery_status;
    }
}
