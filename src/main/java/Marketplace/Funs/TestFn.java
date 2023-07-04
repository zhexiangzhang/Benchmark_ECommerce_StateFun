package Marketplace.Funs;

import Marketplace.Constant.Constants;
import Marketplace.Types.MsgForTesting.EmptyState;
import Marketplace.Types.State.StockState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.shaded.com.google.protobuf.Empty;

import java.util.concurrent.CompletableFuture;

public class TestFn implements StatefulFunction {

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "test");

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withSupplier(TestFn::new)
            .build();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        if (message.is(EmptyState.TYPE)) {
//            onEmptyState(context, message);
            System.out.println("TestFn: onEmptyState");
        }
        return context.done();
    }
}

