package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
