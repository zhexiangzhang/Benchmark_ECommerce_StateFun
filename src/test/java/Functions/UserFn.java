package Functions;

import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

public class UserFn {

    static final TypeName TYPENAME = TypeName.typeNameOf("java.showcase.fns", "user");

    public static final ValueSpec<Integer> SEEN_INT_SPEC =
            ValueSpec.named("seen").withIntType();

    public static final StatefulFunctionSpec GREET_FN_SPEC =
            StatefulFunctionSpec.builder(TYPENAME)
                    .withValueSpec(SEEN_INT_SPEC)
                    .withSupplier(SimpleGreeter::new)
                    .build();

//    @Test
//    public void loggingTest(){
//
//    }

    private static final class SimpleGreeter implements StatefulFunction {

        @Override
        public CompletableFuture<Void> apply(Context context, Message argument) {
            int seen = context.storage().get(SEEN_INT_SPEC).orElse(0);

            System.out.println("Seen so far: "+seen);

            context.storage().set(SEEN_INT_SPEC, seen + 1);

            return CompletableFuture.completedFuture(null);
        }
    }

}
