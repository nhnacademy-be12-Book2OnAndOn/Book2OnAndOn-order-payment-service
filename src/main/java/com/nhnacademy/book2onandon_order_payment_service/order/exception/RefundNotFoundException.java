package com.nhnacademy.book2onandon_order_payment_service.order.exception;

public class RefundNotFoundException extends RuntimeException {
    public RefundNotFoundException(String message) {
        super(message);
    }
}