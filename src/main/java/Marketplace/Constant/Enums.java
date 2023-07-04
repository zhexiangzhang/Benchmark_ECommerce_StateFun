package Marketplace.Constant;

public class Enums {
    public enum TaskType
    {
        //
        AddProductType,
        UpdatePriceType,
        GetAllProductsType,
        DeleteProductType,
        // OrderFn
        AttemptReservationsType,
        ConfirmReservationsType,
        CancelReservationsType
    };

    public enum SendType
    {
        ProductFn,
        StockFn,
        None
    };

    public enum ItemStatus
    {
        DELETED, // deleted from DB
        OUT_OF_STOCK, //
        PRICE_DIVERGENCE,
        IN_STOCK,
        // Strictly speaking it is not item status, but used as a placeholder.
        UNKNOWN
    };

    public enum OrderStatus
    {
        // OrderFn
        CREATED,
        PROCESSING,
        CANCLED,
        INVOICED,
        SHIPPED,
        DELIVERED,
        PAYMENT_FAILED,
        PAYMENT_SUCCESS,
        PAYMENT_PROCESSED
    };

    public enum PackageStatus
    {
        CREATED,
        SHIPPED,
        DELIVERED
    }

    public enum NotificationType
    {
        notify_failed_payment,
        notify_success_payment,
        notify_shipment,
        notfiy_delivered
    }

    public enum ShipmentStatus
    {
        CONCLUDED,
        DELIVERY_IN_PROGRESS
    }
}
