package Common.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Entity not present in olist original data set
 * Thus, the basket item entity is derived from
 * the needs to process the order.
 * This could include the freight value...
 */

@Data
public class BasketItem {

    @JsonProperty("productId") private long productId;
    @JsonProperty("sellerId") private long sellerId;
    @JsonProperty("unitPrice") private double unitPrice;
//    @JsonProperty("oldUnitPrice") private double oldUnitPrice;
//    @JsonProperty("freightValue") private double freightValue; //运费
    @JsonProperty("quantity") private int quantity;
//    @JsonProperty("unavailable") private boolean unavailable;
}
