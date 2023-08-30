package Marketplace.Constant;

public class Constants {
    public static final String TYPES_NAMESPACE = "e-commerce.types";
    public static final String FUNS_NAMESPACE = "e-commerce.fns";
    public static final String EGRESS_NAMESPACE = "io.statefun.playground";
    public static final int no = 1000; // 瞎写的
    public static final int nProductPartitions = 1000;
    public static final int nShipmentPartitions = 1000;
    public static final int nOrderPartitions = 1000;
    public static final int nStockPartitions = 1000;
    public static final int nPaymentPartitions = 1000;
//    public static final int nCustomerPartitions = 1000000;
}


//{
//        "statefunNamespace": "/e-commerce.fns/",
//        "statefunHttpContentType": "application/vnd.e-commerce.types/",
//        "statefunPartitionConfig": {
//        "stockPartion": 10,
//        "productPartion": 10,
//        "customerPartion": 10,
//        "sellerPartion": 10,
//        "shipmentPartion": 10,
//        "orderPartion": 10,
//        "paymentPartion": 10
//        }
//        }