package Marketplace.Funs;

import Common.Entity.BasketItem;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToShipment.ProcessShipment;
import Marketplace.Types.MsgToShipment.UpdateShipment;
import Marketplace.Types.MsgToShipmentProxy.UpdateShipments;
import Marketplace.Types.MsgToStock.CheckoutResv;
import Marketplace.Types.State.ProductState;
import Marketplace.Types.State.ShipmentProxyState;
import Marketplace.Types.State.ShipmentState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ShipmentProxyFn implements StatefulFunction {

    Logger logger = Logger.getLogger("ShipmentProxyFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "shipmentProxy");

//    static final ValueSpec<Long> SHIPMENTIDSTATE = ValueSpec.named("shipmentIdState").withLongType();
    static final ValueSpec<ShipmentProxyState> PROXYSTATE =  ValueSpec.named("shipmentProxyState").withCustomType(ShipmentProxyState.TYPE);

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(PROXYSTATE)
            .withSupplier(ShipmentProxyFn::new)
            .build();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            if (message.is(UpdateShipments.TYPE)) {
                onUpdateShipments(context, message);
            }
            // ack from shipment
            else if (message.is(UpdateShipment.TYPE)) {
                onProcessPartitionAck(context, message);
            }
        } catch (Exception e) {
            logger.info("Exception in ShipmentProxyFn: " + e.getMessage());
        }
        return context.done();
    }

    private void onUpdateShipments(Context context, Message message) {
        UpdateShipments updateShipments = message.as(UpdateShipments.TYPE);
        int tid = updateShipments.getTid();
        ShipmentProxyState shipmentProxyState = context.storage().get(PROXYSTATE).orElse(new ShipmentProxyState());

        int partitionNum = Constants.nShipmentPartitions;
        shipmentProxyState.addTask(tid, partitionNum);
        logger.info("[receive] ShipmentProxyFn: Updated shipmentProxyState, tid = " + tid);

        // 循环partitionNum次，每次发送一个UpdateShipment
        for (int i = 0; i < partitionNum; i++) {
            // TODO: 7/6/2023
            sendMessage(context,
                    ShipmentFn.TYPE,
                    String.valueOf(i),
                    UpdateShipment.TYPE,
                    new UpdateShipment(tid));
        }

        context.storage().set(PROXYSTATE, shipmentProxyState);
//        logger.info("ShipmentProxyFn: Updated shipmentProxyState, tid = " + tid);
    }

    private void onProcessPartitionAck(Context context, Message message) {
        ShipmentProxyState shipmentProxyState = context.storage().get(PROXYSTATE).orElse(new ShipmentProxyState());

        UpdateShipment updateShipment = message.as(UpdateShipment.TYPE);
        int tid = updateShipment.getTid();

        shipmentProxyState.subTaskDone(tid);

        if (shipmentProxyState.isTaskDone(tid)) {
            shipmentProxyState.removeTask(tid);
            logger.info("ShipmentProxyFn: All partitions acked, tid = " + tid);
        }
    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }
}
