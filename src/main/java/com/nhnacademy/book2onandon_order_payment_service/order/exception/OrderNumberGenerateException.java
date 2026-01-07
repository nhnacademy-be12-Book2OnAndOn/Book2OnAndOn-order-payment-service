package com.nhnacademy.book2onandon_order_payment_service.order.exception;

public class OrderNumberGenerateException extends RuntimeException {

    public OrderNumberGenerateException(String message) {
        super(message);
    }

    public OrderNumberGenerateException(String message, Throwable cause) {
        super(message, cause);
    }
}
