package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

public class NotFoundOrderException extends RuntimeException {
    public NotFoundOrderException(String message) {
        super(message);
    }
}
