package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

public class DeliveryNotFoundException extends RuntimeException {
    public DeliveryNotFoundException() {
        super("배송정보를 찾을 수 없습니다.");
    }
    public DeliveryNotFoundException(Long deliveryId) {
        super("배송정보를 찾을 수 없습니다. (deliveryId: " + deliveryId + ")");
    }
    public DeliveryNotFoundException(String message) {
        super(message);
    }
}