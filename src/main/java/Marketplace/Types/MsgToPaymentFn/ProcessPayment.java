package Marketplace.Types.MsgToPaymentFn;

import Common.Entity.Invoice;
import Marketplace.Constant.Constants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

@Setter
@Getter
public class ProcessPayment {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Type<ProcessPayment> TYPE =
            SimpleType.simpleImmutableTypeFrom(
                    TypeName.typeNameOf(Constants.TYPES_NAMESPACE, "ProcessPayment"),
                    mapper::writeValueAsBytes,
                    bytes -> mapper.readValue(bytes, ProcessPayment.class));

    @JsonProperty("invoice")
    private Invoice invoice;

    public ProcessPayment() {
    }

    @JsonCreator
    public ProcessPayment(@JsonProperty("invoice") Invoice invoice) {
        this.invoice = invoice;
    }
}
