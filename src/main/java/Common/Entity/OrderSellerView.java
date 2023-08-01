package Common.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSellerView {
    @JsonProperty("sellerId") private long sellerId;

    // information below from a seller's perspective

    @JsonProperty("count_items") private int count_items = 0;
    @JsonProperty("total_amount") private double total_amount = 0;
    @JsonProperty("total_freight") private double total_freight = 0;
    @JsonProperty("total_incentive") private double total_incentive = 0;
    @JsonProperty("total_invoice") private double total_invoice = 0;
    @JsonProperty("total_items") private double total_items = 0;

    @JsonCreator
    public OrderSellerView() {
    }

    @JsonCreator
    public OrderSellerView(
            @JsonProperty("sellerId") long sellerId,
            @JsonProperty("count_items") int count_items,
            @JsonProperty("total_amount") double total_amount,
            @JsonProperty("total_freight") double total_freight,
            @JsonProperty("total_incentive") double total_incentive,
            @JsonProperty("total_invoice") double total_invoice,
            @JsonProperty("total_items") double total_items) {
          this.sellerId = sellerId;
          this.count_items = count_items;
          this.total_amount = total_amount;
          this.total_freight = total_freight;
          this.total_incentive = total_incentive;
          this.total_invoice = total_invoice;
          this.total_items = total_items;
     }
}
