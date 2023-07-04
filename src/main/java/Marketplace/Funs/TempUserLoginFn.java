package Marketplace.Funs;

import static Marketplace.Types.Messages.USER_LOGIN_JSON_TYPE;
import static Marketplace.Types.Messages.USER_PROFILE_JSON_TYPE;

import Marketplace.Types.Entity.TmpUserLogin;
import Marketplace.Types.Entity.TmpUserPofile;
import Marketplace.Constant.Constants;

import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.io.KafkaEgressMessage;
import org.apache.flink.statefun.sdk.java.message.EgressMessageBuilder;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.EgressMessage;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Types;


import java.util.concurrent.CompletableFuture;

public class TempUserLoginFn implements StatefulFunction{

    //  注册函数，逻辑名称=<namespace>+<name>
//    static final TypeName TYPE = TypeName.typeNameFromString("e-commerce.fns/login"); // 和下面的等价
    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "login");

//  存储的状态值
    static final ValueSpec<Integer> SEEN = ValueSpec.named("seen").withIntType();

//    static final TypeName CUSTOM_EGRESS = TypeName.typeNameOf("e-commerce.fns", "custom-sink");

    static final TypeName KFK_EGRESS = TypeName.typeNameOf("e-commerce.fns", "kafkaSink");

    //  包含了创建函数实例所需的所有信息
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(SEEN) // 如果是两个，就是.withValueSpecs(SEEN, SEEN2)
            .withSupplier(TempUserLoginFn::new)
            .build();

//    DummyPaymentServiceService dummyPaymentService = new DummyPaymentServiceService();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message){
        if (message.is(USER_LOGIN_JSON_TYPE)){
            final TmpUserLogin tmpUserLogin = message.as(USER_LOGIN_JSON_TYPE);

            int seen = context.storage().get(SEEN).orElse(0);
            context.storage().set(SEEN, seen + 1);
            System.out.println("function: tempUserLoginFn, seen: " + tmpUserLogin.getUsername() + " " + seen);
            final TmpUserPofile tmpUserPofile = new TmpUserPofile(tmpUserLogin.getUsername(), seen + 1);

            context.send(
                    KafkaEgressMessage.forEgress(KFK_EGRESS)
                            .withTopic("hello")
                            .withUtf8Key(context.self().id())
                            .withUtf8Value("Hello KAFKA" + tmpUserLogin.getUsername() + " for the " + seen + "th time!")
                            .build());
        }  else if (message.is(Types.stringType())) {
            System.out.println("function: tempUserLoginFn, message: CYFCFY");
        }


        return context.done();
    }
}
