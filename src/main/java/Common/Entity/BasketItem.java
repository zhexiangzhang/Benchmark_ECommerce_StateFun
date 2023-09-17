package Common.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity not present in olist original data set
 * Thus, the basket item entity is derived from
 * the needs to process the order.
 * This could include the freight value...
 */


@Setter
@Getter
public class BasketItem {

    @JsonProperty("SellerId") private long sellerId;
    @JsonProperty("ProductId") private long productId;

    @JsonProperty("ProductName") private String productName;
    @JsonProperty("UnitPrice") private double unitPrice;
    @JsonProperty("FreightValue") private double freightValue; //运费

    @JsonProperty("Quantity") private int quantity;
    @JsonProperty("Voucher") private double vouchers;
    @JsonProperty("Version") private int version;

    public BasketItem() {
    }

    @JsonCreator
    public BasketItem(@JsonProperty("SellerId") long sellerId,
                      @JsonProperty("ProductId") long productId,
                      @JsonProperty("ProductName") String productName,
                      @JsonProperty("UnitPrice") double unitPrice,
                      @JsonProperty("FreightValue") double freightValue,
                      @JsonProperty("Quantity") int quantity,
                      @JsonProperty("Voucher") double vouchers,
                      @JsonProperty("Version") int version
                      ) {
        this.sellerId = sellerId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.freightValue = freightValue;
        this.quantity = quantity;
        this.vouchers = vouchers;
        this.version = version;
    }

}
