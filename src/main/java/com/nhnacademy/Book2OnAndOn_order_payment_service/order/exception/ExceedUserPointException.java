package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

public class ExceedUserPointException extends RuntimeException {
    public ExceedUserPointException(String message) {
        super(message);
    }
}
