package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

public class RefundNotFoundException extends RuntimeException {
    public RefundNotFoundException(String message) {
        super(message);
    }
}