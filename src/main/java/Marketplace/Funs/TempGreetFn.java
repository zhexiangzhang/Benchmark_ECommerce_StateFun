package Marketplace.Funs;

import static Marketplace.Types.Messages.USER_PROFILE_JSON_TYPE;
import static Marketplace.Types.Messages.EGRESS_RECORD_JSON_TYPE;

import Marketplace.Constant.Constants;
import Marketplace.Types.EgressRecord;
import Marketplace.Types.Entity.TmpUserPofile;

import Marketplace.Types.Messages;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.EgressMessage;
import org.apache.flink.statefun.sdk.java.message.EgressMessageBuilder;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Types;


import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TempGreetFn implements StatefulFunction {
    //  注册函数，逻辑名称=<namespace>+<name>
//    static final TypeName TYPE = TypeName.typeNameFromString("e-commerce.fns/greet");
    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "greet");

    //  包含了创建函数实例所需的所有信息
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withSupplier(TempGreetFn::new)  // 如果是两个，就是.withValueSpecs(SEEN, SEEN2)
            .build();

    private static final TypeName ECOMMERCE_EGRESS = TypeName.typeNameOf(Constants.EGRESS_NAMESPACE, "egress");

 /*
    This function demonstrates performing asynchronous operations during a function invocation. It
    is a common scenario for functions to have external dependencies in order for it to complete its
    work, such as fetching enrichment information from remote databases.
 */

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws InterruptedException {
        if (message.is(USER_PROFILE_JSON_TYPE)) {
            final TmpUserPofile userPofile = message.as(USER_PROFILE_JSON_TYPE);
            final String greetings = String.format("Hello, %s!, cnt %s", userPofile.getUsername(), userPofile.getLoginCnt());
            final EgressRecord egressRecord = new EgressRecord("greeting", greetings);
            System.out.println(userPofile.getUsername() + "【main】 start async remote call");
            final CompletableFuture<Integer> ageResult = remote(userPofile.getUsername());

//            final CompletableFuture<Integer> ageResult = testA.remote(userPofile.getUsername());
//            ageResult.thenAccept(
//                    result -> {
//                        System.out.println("【main】 async remote call complete " + userPofile.getUsername());
//                        // 在异步操作完成后进行处理
//                        // 处理异步操作的结果或执行其他操作
//                    }
//            );


//            return CompletableFuture.allOf(ageResult).whenCompleteAsync(
//                    (result, throwable) -> {
//                        System.out.println("【main】 async remote call complete " + userPofile.getUsername() + " result: " + result);
//                        // 在异步操作完成后进行处理
//                        // 处理异步操作的结果或执行其他操作
//                        final Optional<Address> caller = context.caller();
//                        context.send(
//                                MessageBuilder.forAddress(caller.get())
//                                        .withCustomType(Types.stringType(), "async")
//                                        .build());
//                        System.out.println("------------------");
//                    }
//            );


            ageResult.whenComplete(
                    (result, throwable) -> {
                        System.out.println("【main】 async remote call complete " + userPofile.getUsername() + " result: " + result);
                        // 在异步操作完成后进行处理
                        // 处理异步操作的结果或执行其他操作
                        final Optional<Address> caller = context.caller();
                        context.send(
                                MessageBuilder.forAddress(caller.get())
                                        .withCustomType(Types.stringType(), "async")
                                        .build());
                        System.out.println("------------------");
                    }
            );

//            System.out.println("【main】 done + " + userPofile.getUsername());
//            for (int i = 0; i < 10; i++) {
//                Thread.sleep(1000);
//                System.out.println("【main】 function: ageresu, i: " + i + userPofile.getUsername());
//            }

        }
        return context.done();
    }


    private CompletableFuture<Integer> remote(String username) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        System.out.println("【remote】 " + username + " is start.");
                        Thread.sleep(2000);
                        System.out.println("【remote】 " + username + " is sleeping for 2s.");
                        Thread.sleep(2000);
                        System.out.println("【remote】 " + username + " is sleeping for 3s.");
                        Thread.sleep(2000);
                        System.out.println("【remote】 " + username + " is sleeping for 4s.");
                        Thread.sleep(2000);
                        System.out.println("【remote】 " + username + " is sleeping for 5s.");
                    } catch (InterruptedException e) {
                        throw new RuntimeException();
                    }
                    return 29;
                });
    }
}
