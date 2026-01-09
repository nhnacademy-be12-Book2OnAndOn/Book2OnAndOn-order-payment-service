package com.nhnacademy.book2onandon_order_payment_service.order.exception;

public class OrderNotCancellableException extends RuntimeException {
    public OrderNotCancellableException(String message) {
        super(message);
    }
}
