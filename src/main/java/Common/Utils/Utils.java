package Common.Utils;

import Common.Entity.TransactionMark;
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

import java.util.Optional;

public class Utils {

    static final TypeName KFK_EGRESS = TypeName.typeNameOf("e-commerce.fns", "kafkaSink");

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
                                                 long taskId, int tid, String receiver, String result) {
        String response = "";
        try {
            TransactionMark transactionMark = new TransactionMark(taskId, tid, receiver, result);
            ObjectMapper mapper = new ObjectMapper();
            response = mapper.writeValueAsString(transactionMark);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        context.send(
                KafkaEgressMessage.forEgress(KFK_EGRESS)
                        .withTopic(transactionType)
                        .withUtf8Key(functionID)
                        .withUtf8Value(response)
                        .build());
    }

    ////        System.out.println("checkout result send to kafka");

}
