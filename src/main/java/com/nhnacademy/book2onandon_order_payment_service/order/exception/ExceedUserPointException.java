package com.nhnacademy.book2onandon_order_payment_service.order.exception;

public class ExceedUserPointException extends RuntimeException {
    public ExceedUserPointException(String message) {
        super(message);
    }
}
