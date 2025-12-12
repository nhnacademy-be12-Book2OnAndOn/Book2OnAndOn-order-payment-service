package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.NotFoundException;

public class DeliveryNotFoundException extends NotFoundException {
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