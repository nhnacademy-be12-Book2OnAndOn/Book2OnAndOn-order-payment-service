package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

public class InvalidDeliveryDateException extends RuntimeException {
    public InvalidDeliveryDateException(String message) {
        super(message);
    }
}
