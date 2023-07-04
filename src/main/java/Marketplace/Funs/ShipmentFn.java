package Marketplace.Funs;

import Common.Entity.*;
import Marketplace.Constant.Constants;
import Marketplace.Constant.Enums;
import Marketplace.Types.MsgToCustomer.NotifyCustomer;
import Marketplace.Types.MsgToOrderFn.OrderStateUpdate;
import Marketplace.Types.MsgToShipment.GetPendingPackages;
import Marketplace.Types.MsgToShipment.ProcessShipment;
import Marketplace.Types.MsgToShipment.UpdateShipment;
import Marketplace.Types.State.ShipmentState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.Type;
import org.apache.flink.statefun.sdk.java.types.Types;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ShipmentFn implements StatefulFunction {

    Logger logger = Logger.getLogger("ShipmentFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "shipment");

    static final ValueSpec<Long> SHIPMENTIDSTATE = ValueSpec.named("shipmentIdState").withLongType();
    static final ValueSpec<ShipmentState> SHIPMENT_STATE = ValueSpec.named("shipmentState").withCustomType(ShipmentState.TYPE);

    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpecs(SHIPMENTIDSTATE, SHIPMENT_STATE)
            .withSupplier(ShipmentFn::new)
            .build();

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            if (message.is(ProcessShipment.TYPE)) {
                onProcessShipment(context, message);
            } else if (message.is(GetPendingPackages.TYPE)) {
                onGetPendingPackages(context, message);
            } else if (message.is(UpdateShipment.TYPE)) {
                onUpdateShipment(context, message);
            }
        } catch (Exception e) {
            System.out.println("ShipmentFn Exception !!!!!!!!!!!!!!!!");
        }
        return context.done();
    }

    private Long generateNextShipmentId(Context context) {
        Long shipmentId = context.storage().get(SHIPMENTIDSTATE).orElse(0L) + 1;
        context.storage().set(SHIPMENTIDSTATE, shipmentId);
        return shipmentId;
    }

    private String getPartionText(String id) {
        return String.format("[ ShipmentFn partitionId %s ] ", id);
    }

    private ShipmentState getShipmentState(Context context) {
        return context.storage().get(SHIPMENT_STATE).orElse(new ShipmentState());
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private void showLogPrt(String log) {
//        logger.info(log);
        System.out.println(log);
    }

    private void onProcessShipment(Context context, Message message) {
        ProcessShipment processShipment = message.as(ProcessShipment.TYPE);
        Invoice invoice = processShipment.getInvoice();
        CustomerCheckout customerCheckout = invoice.getCustomerCheckout();

        String log = getPartionText(context.self().id()) + "ShipmentFn: onProcessShipment: " + processShipment.toString();
        logger.info(log);

        // The Java equivalent code
        List<OrderItem> invoiceItems = invoice.getItems();
        List<OrderItem> items = invoiceItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getSellerId))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, List<OrderItem>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());

        long shipmentId = generateNextShipmentId(context);
        Shipment shipment = new Shipment(
                shipmentId,
                invoice.getOrder().getId(),
                Long.parseLong(invoice.getOrderPartitionID()),
                customerCheckout.getCustomerId(),
                customerCheckout.getName(),
                items.size(),
                customerCheckout.getAddress(),
                customerCheckout.getZipCode(),
                invoice.getOrder().getPurchaseTimestamp(),
                Enums.PackageStatus.CREATED.toString()
        );

        int packageId = 1;
//        List<OrderItem> orderItems = invoice.getItems();
        List<PackageItem> packages = new ArrayList<>();
        for (OrderItem orderItem : items) {
            PackageItem pkg = new PackageItem(
                    packageId,
                    shipmentId,
                    orderItem.getSellerId(),
                    orderItem.getProductId(),
                    orderItem.getQuantity(),
                    Enums.PackageStatus.SHIPPED
            );
            packages.add(pkg);
            packageId++;
        }

        ShipmentState shipmentState = getShipmentState(context);
        shipmentState.addShipment(shipmentId, shipment);
        shipmentState.addPackage(shipmentId, packages);
        context.storage().set(SHIPMENT_STATE, shipmentState);

        /**
         * Based on olist (https://dev.olist.com/docs/orders), the status of the order is
         * shipped when "at least one order item has been shipped"
         * All items are considered shipped here, so just signal the order about that
         */

        String orderPartition = invoice.getOrderPartitionID();
        sendMessage(
                context,
                OrderFn.TYPE,
                String.valueOf(orderPartition),
                OrderStateUpdate.TYPE,
                new OrderStateUpdate(
                        invoice.getOrder().getId(),
                        Enums.OrderStatus.SHIPPED
                )
        );

        // send message to customer
        NotifyCustomer notifyCustomer = new NotifyCustomer(
                customerCheckout.getCustomerId(),
                invoice.getOrder(),
                Enums.NotificationType.notify_shipment
        );
        notifyCustomer.setNumDeliveries(packages.size());
        sendMessage(
                context,
                CustomerFn.TYPE,
                String.valueOf(customerCheckout.getCustomerId() % Constants.nCustomerPartitions),
                NotifyCustomer.TYPE,
                notifyCustomer
        );

        String log_ = getPartionText(context.self().id())
                + "order is shipped, send message to orderFn and customerFn\n";
        showLog(log_);

        // TODO: 6/12/2023 whether need to confirm ????

    }

    private void onGetPendingPackages(Context context, Message message) {
        long sellerId = message.as(GetPendingPackages.TYPE).getSellerID();
        ShipmentState shipmentState = getShipmentState(context);
        Map<Long, List<PackageItem>> packages = shipmentState.getPackages();

        List<PackageItem> pendingPackages =
                packages.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.getPackageStatus().equals(Enums.PackageStatus.SHIPPED) && p.getSellerId() == sellerId)
                .collect(Collectors.toList());


        String pendingPackagesStr = pendingPackages.stream()
                .map(PackageItem::toString)
                .collect(Collectors.joining("\n"));

        String log = getPartionText(context.self().id()) + "PendingPackages: \n" + pendingPackagesStr + "\n";
        logger.info(log);
    }

    /**
     * get the oldest (OPEN) shipment per seller
     * select seller id, min(shipment id)
     * from packages
     * where packages.status == shipped
     * group by seller id
     * 
     */
    // TODO: 6/30/2023 we only need update the oldest? or should we update by seller id
    // 每次更新整个shipment，shipment包含多个package
    private void onUpdateShipment(Context context, Message message) {
        System.out.println(getPartionText(context.self().id()) + " onUpdateShipment\n");

        ShipmentState shipmentState = getShipmentState(context);
        Map<Long, List<PackageItem>> packages = shipmentState.getPackages();

        // contains the minimum shipment ID for each seller.
        // 对应 每个卖家（sellerId）对应的最小发货单号（shipmentId）
        Map<Long, Long> q = packages.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.getPackageStatus().equals(Enums.PackageStatus.SHIPPED))
                .collect(Collectors.groupingBy(PackageItem::getSellerId,
                        Collectors.minBy(Comparator.comparingLong(PackageItem::getShipmentId))))
                .entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get().getShipmentId()));

        String log = getPartionText(context.self().id())
                + "Shipments to update: (sellerID-MinShipmentID) " + q.toString() + "\n";
        System.out.println(log);

//        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<Long, Long> kv : q.entrySet()) {
            // 获取相应的包裹列表
            List<PackageItem> packagesForSeller = packages.get(kv.getValue()).stream()
                    .filter(p -> p.getSellerId() == kv.getKey() && p.getPackageStatus().equals(Enums.PackageStatus.SHIPPED))
                    .collect(Collectors.toList());
//            futures.add(updatePackageDelivery(packagesForSeller, context));
            updatePackageDelivery(packagesForSeller, context);
        }

        String log_ = getPartionText(context.self().id())
                + "Update Shipment finished\n";
        showLog(log_);

        context.storage().set(SHIPMENT_STATE, shipmentState);
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                .thenRun(() ->
////                        System.out.println("Update Shipment finished");
//                        sendMessage(
//                                context,
//                                TempUserLoginFn.TYPE,
//                                String.valueOf(1),
//                                Types.stringType(),
//                                "Update Shipment finished"
//                        )
//                );
    }

    private void updatePackageDelivery(List<PackageItem> packageToUpdate, Context context) {
        ShipmentState shipmentState = getShipmentState(context);
        Map<Long, Shipment> shipments = shipmentState.getShipments();
        Map<Long, List<PackageItem>> packages = shipmentState.getPackages();
        long shipmentId = packageToUpdate.get(0).getShipmentId();
        long sellerId = packageToUpdate.get(0).getSellerId();

        // aggregate operation
//        计算指定发货单号下的已交付的包裹数量
        int countDelivered = (int) packages.get(shipmentId).stream()
                .filter(p -> p.getPackageStatus() == Enums.PackageStatus.DELIVERED)
                .count();

        String log = getPartionText(context.self().id())
                + " -- Count delivery for shipment id " + shipmentId
                + ": " + countDelivered + " total of " + shipments.get(shipmentId).getPackageCnt() + "\n";
        System.out.println(log);

        LocalDateTime now = LocalDateTime.now();
        for (PackageItem p : packageToUpdate) {
            p.setPackageStatus(Enums.PackageStatus.DELIVERED);
            p.setDelivered_time(now);
        }

//      only single customer per shipment
        long customerId = shipments.get(shipmentId).getCustomerId();
        // send message to customer
        NotifyCustomer notifyCustomer = new NotifyCustomer(
                customerId,
                null,
                Enums.NotificationType.notfiy_delivered
        );
        notifyCustomer.setNumDeliveries(packageToUpdate.size());
        sendMessage(
                context,
                CustomerFn.TYPE,
                String.valueOf(customerId % Constants.nCustomerPartitions),
                NotifyCustomer.TYPE,
                notifyCustomer
        );

        Shipment shipment = shipments.get(shipmentId);
        if (shipment.getPackageCnt() == countDelivered + packageToUpdate.size()) {
            long orderPartition = shipment.getOrderPartition();
            long orderId = shipment.getOrderId();
            sendMessage(
                    context,
                    OrderFn.TYPE,
                    String.valueOf(orderPartition),
                    OrderStateUpdate.TYPE,
                    new OrderStateUpdate(
                            orderId,
                            Enums.OrderStatus.DELIVERED
                    )
            );
            shipment.setPackageStatus(Enums.ShipmentStatus.CONCLUDED.toString());
        }
        String log_ = getPartionText(context.self().id())
                + "updatePackageDelivery for packages: \n" + packages.toString() + "\n";
        System.out.println(log_);
    }
//    private CompletableFuture<Void> updatePackageDelivery(List<PackageItem> packages, Context context) {
//        return CompletableFuture.runAsync(() -> {
//            ShipmentState shipmentState = getShipmentState(context);
//            Map<Long, Shipment> shipments = shipmentState.getShipments();
//            Map<Long, List<PackageItem>> packagesMap = shipmentState.getPackages();
//            long shipmentId = packages.get(0).getShipmentId();
//            long customerId = shipments.get(shipmentId).getCustomerId();
//            // send message to customer
//            sendMessage(
//                    context,
//                    CustomerFn.TYPE,
//                    String.valueOf(customerId),
//                    NotifyCustomer.TYPE,
//                    new NotifyCustomer(
//                            customerId,
//                            null,
//                            Enums.NotificationType.notfiy_delivered
//                    )
//            );
//            System.out.println("updatePackageDelivery for packages: " + packages.toString());
//        });
//    }

    private <T> void sendMessage(Context context, TypeName addressType, String addressId, Type<T> messageType, T messageContent) {
        Message msg = MessageBuilder.forAddress(addressType, addressId)
                .withCustomType(messageType, messageContent)
                .build();
        context.send(msg);
    }
}
