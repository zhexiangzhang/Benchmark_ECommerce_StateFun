package Marketplace.Funs;

import Common.Entity.*;
import Common.Utils.Utils;
import Marketplace.Constant.Enums;
import Marketplace.Constant.Constants;
import Marketplace.Types.MsgToOrderFn.PaymentNotification;
import Marketplace.Types.MsgToOrderFn.ShipmentNotification;
import Marketplace.Types.MsgToPaymentFn.InvoiceIssued;
import Marketplace.Types.MsgToSeller.*;
import Marketplace.Types.State.SellerState;
import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.types.Types;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class SellerFn implements StatefulFunction {

    Logger logger = Logger.getLogger("SellerFn");

    static final TypeName TYPE = TypeName.typeNameOf(Constants.FUNS_NAMESPACE, "seller");

    static final ValueSpec<SellerState> SELLERSTATE = ValueSpec.named("sellerState").withCustomType(SellerState.TYPE);
    //  Contains all the information needed to create a function instance
    public static final StatefulFunctionSpec SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpecs(SELLERSTATE)
            .withSupplier(SellerFn::new)
            .build();

    private String getPartionText(String id) {
        return String.format(" [ SellerFn partitionId %s ] ", id);
    }

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
        try {
            // client ---> seller (init seller type)
            if (message.is(InitSeller.TYPE)) {
                onInitSeller(context, message);
            }
            // client ---> seller (get seller type)
            else if (message.is(GetSeller.TYPE)) {
                onGetSeller(context);
            }
            // order ---> seller
            else if (message.is(InvoiceIssued.TYPE)) {
                ProcessNewInvoice(context, message);
            }
            // client ---> seller (increase stock)
            else if (message.is(IncreaseStock.TYPE)) {
                onIncrStockAsyncBegin(context, message);
            }
            // driver ----> query dashboard
            else if (message.is(QueryDashboard.TYPE)) {
                onQueryDashboard(context, message);
            }
            else if (message.is(PaymentNotification.TYPE)) {
                UpdateOrderStatus(context, message);
            }
            else if (message.is(ShipmentNotification.TYPE)) {
                ProcessShipmentNotification(context, message);
            }
            else if (message.is(DeliveryNotification.TYPE)) {
                ProcessDeliveryNotification(context, message);
            }
            // xxxxx ---> seller
            else if (message.is(Types.stringType())) {
                String result = message.as(Types.stringType());
                Long sellerId = Long.parseLong(context.self().id());
                String log = String.format(getPartionText(context.self().id())
                                + "sellerId: %s, result: %s\n",
                        sellerId, result);
                showLog(log);
            }

        } catch (Exception e) {
            System.out.println("SellerFn apply error !!!!!!!!!!!!!!!");
            e.printStackTrace();
        }

        return context.done();
    }

    private void showLog(String log) {
        logger.info(log);
//        System.out.println(log);
    }

    private void printLog(String log) {
        System.out.println(log);
    }

    private SellerState getSellerState(Context context) {
        return context.storage().get(SELLERSTATE).orElse(new SellerState());
    }

    private void onInitSeller(Context context, Message message) {
        InitSeller initSeller = message.as(InitSeller.TYPE);
        Seller seller = initSeller.getSeller();
        SellerState sellerState = getSellerState(context);
        sellerState.setSeller(seller);

        context.storage().set(SELLERSTATE, sellerState);

        String log = String.format(getPartionText(context.self().id())
                + "init seller success, sellerId: %s\n", seller.getId());
        printLog(log);
    }

    private void onGetSeller(Context context) {
        SellerState sellerState = getSellerState(context);
        Seller seller = sellerState.getSeller();

        String log = String.format(getPartionText(context.self().id())
                + "get seller success\n"
                + seller.toString()
                + "\n"
        );
        showLog(log);
    }

    private void onIncrStockAsyncBegin(Context context, Message message) {
        IncreaseStock increaseStock = message.as(IncreaseStock.TYPE);
        StockItem stockItem = increaseStock.getStockItem();
        long productId = stockItem.getProduct_id();
        int prodFnPartitionID = (int) (productId % Constants.nProductPartitions);
//        sendGetProdMsgToProdFn(context, increaseStock, prodFnPartitionID);
        Utils.sendMessage(context,
                ProductFn.TYPE,
                String.valueOf(prodFnPartitionID),
                IncreaseStock.TYPE,
                increaseStock);
    }

    private void ProcessNewInvoice(Context context, Message message) {

        SellerState sellerState = getSellerState(context);
        InvoiceIssued invoiceIssued = message.as(InvoiceIssued.TYPE);

        Invoice invoice = invoiceIssued.getInvoice();
        List<OrderItem> orderItems = invoice.getItems();
        long sellerId = sellerState.getSeller().getId();
        long orderId = invoice.getOrderID();

        for (OrderItem orderItem : orderItems) {
            OrderEntry orderEntry = new OrderEntry(
                    orderId,
                    sellerId,
                    // packageI =
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getQuantity(),
                    orderItem.getTotalAmount(),
                    orderItem.getTotalPrice(),
                    // total_invoice =
                    // total_incentive =
                    orderItem.getFreightValue(),
                    // shipment_date
                    // delivery_date
                    orderItem.getUnitPrice(),
                    Enums.OrderStatus.INVOICED
                    // product_category = ? should come from product
            );
            sellerState.addOrderEntry(orderEntry);
        }

        // order details
        OrderEntryDetails orderEntryDetail = new OrderEntryDetails(
                orderId,
                invoice.getIssueDate(),
                Enums.OrderStatus.INVOICED,
                invoice.getCustomerCheckout().getCustomerId(),
                invoice.getCustomerCheckout().getFirstName(),
                invoice.getCustomerCheckout().getLastName(),
                invoice.getCustomerCheckout().getStreet(),
                invoice.getCustomerCheckout().getComplement(),
                invoice.getCustomerCheckout().getCity(),
                invoice.getCustomerCheckout().getState(),
                invoice.getCustomerCheckout().getZipCode(),
                invoice.getCustomerCheckout().getCardBrand(),
                invoice.getCustomerCheckout().getInstallments()
            );
        sellerState.addOrderEntryDetails(orderId, orderEntryDetail);

        context.storage().set(SELLERSTATE, sellerState);
    }

    private void onQueryDashboard(Context context, Message message) {
        SellerState sellerState = getSellerState(context);

        Set<OrderEntry> orderEntries = sellerState.getOrderEntries();
        Map<Long, OrderEntryDetails> orderEntryDetails = sellerState.getOrderEntryDetails();

        long sellerID = sellerState.getSeller().getId();
        int tid = message.as(QueryDashboard.TYPE).getTid();
//        logger.info("[receive] {tid=" + tid + "} query dashboard, sellerFn " + context.self().id());
        String log = getPartionText(context.self().id())
                + "query dashboard [receive], " + "tid : " + tid + "\n";
        printLog(log);
        // count_items, total_amount, total_freight, total_incentive, total_invoice, total_items
        OrderSellerView orderSellerView = new OrderSellerView(
                sellerID,
                orderEntries.size(),
                orderEntries.stream().mapToDouble(OrderEntry::getTotalAmount).sum(),
                orderEntries.stream().mapToDouble(OrderEntry::getFreight_value).sum(),
                orderEntries.stream().mapToDouble(OrderEntry::getTotalIncentive).sum(),
                orderEntries.stream().mapToDouble(OrderEntry::getTotalInvoice).sum(),
                orderEntries.stream().mapToDouble(OrderEntry::getTotalItems).sum()
        );

        Set<OrderEntry> queryEnTry = new HashSet<>();
        // 1.只保留order_status == OrderStatus.INVOICED || oe.order_status == OrderStatus.READY_FOR_SHIPMENT ||
        //                                                               oe.order_status == OrderStatus.IN_TRANSIT || oe.order_status == OrderStatus.PAYMENT_PROCESSED)
        // 2.将OrderEntryDetails加入到OrderEntry中
        for (OrderEntry oe : orderEntries) {
            Enums.OrderStatus orderStatus = oe.getOrder_status();
            if (orderStatus == Enums.OrderStatus.INVOICED ||
                    orderStatus == Enums.OrderStatus.READY_FOR_SHIPMENT ||
                    orderStatus == Enums.OrderStatus.IN_TRANSIT ||
                    orderStatus == Enums.OrderStatus.PAYMENT_PROCESSED) {
               // 将对应的OrderEntryDetails加入到OrderEntry中
                oe.setOrderEntryDetails(orderEntryDetails.get(oe.getOrder_id()));
                queryEnTry.add(oe);
            }
        }

        SellerDashboard sellerDashboard = new SellerDashboard(
                orderSellerView,
                queryEnTry
        );

        Utils.notifyTransactionComplete(
                context,
                Enums.TransactionType.queryDashboardTask.toString(),
                context.self().id(),
                sellerID, tid, context.self().id(), Enums.MarkStatus.SUCCESS, "seller");

        String log_ = getPartionText(context.self().id())
                + "query dashboard success, " + "tid : " + tid + "\n";
        printLog(log_);
//        logger.info("[success] {tid=" + tid + "} query dashboard, sellerFn " + context.self().id());
    }

    private void UpdateOrderStatus(Context context, Message message) {
        SellerState sellerState = getSellerState(context);
        PaymentNotification orderStateUpdate = message.as(PaymentNotification.TYPE);

        long orderId = orderStateUpdate.getOrderId();
//        if (orderId != sellerState.getSeller().getId()) {
//            throw new RuntimeException("sellerId != orderId");
//        }

        Enums.OrderStatus orderStatus = orderStateUpdate.getOrderStatus();
        sellerState.updateOrderStatus(orderId, orderStatus, null);

        context.storage().set(SELLERSTATE, sellerState);
    }

    private void ProcessShipmentNotification(Context context, Message message) {
        SellerState sellerState = getSellerState(context);
        ShipmentNotification shipmentNotification = message.as(ShipmentNotification.TYPE);
        Enums.OrderStatus orderStatus = null;
        if (shipmentNotification.getShipmentStatus() == Enums.ShipmentStatus.APPROVED) {
            orderStatus = Enums.OrderStatus.READY_FOR_SHIPMENT;
        } else if (shipmentNotification.getShipmentStatus() == Enums.ShipmentStatus.DELIVERY_IN_PROGRESS) {
            orderStatus = Enums.OrderStatus.IN_TRANSIT;
        } else if (shipmentNotification.getShipmentStatus() == Enums.ShipmentStatus.CONCLUDED) {
            orderStatus = Enums.OrderStatus.DELIVERED;
        }

        sellerState.updateOrderStatus(shipmentNotification.getOrderId(), orderStatus, shipmentNotification.getEventDate());


        context.storage().set(SELLERSTATE, sellerState);
//        UpdateOrderStatus(context, orderId, status, eventTime);
    }

    /**
     * Process individual (i.e., each package at a time) delivery notifications
     */
    private void ProcessDeliveryNotification(Context context, Message message) {
        SellerState sellerState = getSellerState(context);
        DeliveryNotification deliveryNotification = message.as(DeliveryNotification.TYPE);

        Set<OrderEntry> orderEntries = sellerState.getOrderEntries();
        for (OrderEntry orderEntry : orderEntries) {
            if (orderEntry.getOrder_id() == deliveryNotification.getOrderId()
                    && orderEntry.getProduct_id() == deliveryNotification.getProductID())
            {
                orderEntry.setDelivery_status(deliveryNotification.getPackageStatus());
                orderEntry.setDelivery_date(deliveryNotification.getEventDate());
                orderEntry.setPackage_id(deliveryNotification.getPackageId());
            }
        }

        context.storage().set(SELLERSTATE, sellerState);
    }
}
