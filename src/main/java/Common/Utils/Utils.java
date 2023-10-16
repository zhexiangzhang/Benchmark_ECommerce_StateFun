package Common.Utils;

import Common.Entity.TransactionMark;
import Marketplace.Constant.Enums;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.Address;
import org.apache.flink.statefun.sdk.java.Context;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.io.KafkaEgressMessage;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;
import org.apache.flink.statefun.sdk.kafka.KafkaTopicPartition;


import java.util.Optional;

public class Utils {

    static final TypeName KFK_EGRESS = TypeName.typeNameOf("e-commerce.fns", "kafkaSink");

//    static final TypeName KFK_EGRESS_Seller = TypeName.typeNameOf("e-commerce.fns", "kafkaSinkSeller");
//    static final TypeName KFK_EGRESS_ShipmentUpd = TypeName.typeNameOf("e-commerce.fns", "kafkaSinkShipmentUpd");
//    static final TypeName KFK_EGRESS_Checkout = TypeName.typeNameOf("e-commerce.fns", "kafkaSinkCheckout");

    public static String getFnName(String fnType) {
        String[] fnTypeArr = fnType.split("/");
        return fnTypeArr[fnTypeArr.length - 1];
    }

    public static <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }

    public static <T> void sendMessageToCaller(Context context, Type<T> messageType, T messageContent) {
        final Optional<Address> caller = context.caller();
        if (caller.isPresent()) {
            context.send(
                    MessageBuilder.forAddress(caller.get())
                            .withCustomType(messageType, messageContent)
                            .build());
        } else {
            throw new IllegalStateException("There should always be a caller");
        }
    }

    public static void notifyTransactionComplete(Context context,
                                                 String transactionType,
                                                 String functionID,
                                                 long taskId, int tid, String receiver, Enums.MarkStatus status, String source) {
        String response = "";
        try {
            TransactionMark transactionMark = new TransactionMark(taskId, tid, receiver, status, source);
            ObjectMapper mapper = new ObjectMapper();
            response = mapper.writeValueAsString(transactionMark);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        TypeName EGRESS_KAFKA = KFK_EGRESS;
//        TypeName EGRESS_KAFKA = null;
//        if (transactionType.equals(Enums.TransactionType.checkoutTask.toString())) {
//            EGRESS_KAFKA = KFK_EGRESS_Checkout;
//        } else if (transactionType.equals(Enums.TransactionType.updateDeliveryTask.toString())) {
//            EGRESS_KAFKA = KFK_EGRESS_ShipmentUpd;
//        } else if (transactionType.equals(Enums.TransactionType.updatePriceTask.toString())) {
//            EGRESS_KAFKA = KFK_EGRESS_Seller;
//        } else if (transactionType.equals(Enums.TransactionType.updateProductTask.toString())) {
//            EGRESS_KAFKA = KFK_EGRESS_Seller;
//        } else if (transactionType.equals(Enums.TransactionType.queryDashboardTask.toString())) {
//            EGRESS_KAFKA = KFK_EGRESS_Seller;
//        } else {
//            throw new IllegalStateException("error in notifyTransactionComplete");
//        }

        context.send(
                KafkaEgressMessage.forEgress(EGRESS_KAFKA)
                        .withTopic(transactionType)
//                        .withUtf8Key(functionID)
                        .withUtf8Key("")  // key is null, so kafka will use round-robin to distribute the message
                        .withUtf8Value(response)
                        .build());
    }

    ////        System.out.println("checkout result send to kafka");

}
