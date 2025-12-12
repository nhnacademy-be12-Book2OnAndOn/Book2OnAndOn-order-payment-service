package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

public class NotFoundDeliveryPolicyException extends RuntimeException {
    public NotFoundDeliveryPolicyException(String message) {
        super(message);
    }
}
