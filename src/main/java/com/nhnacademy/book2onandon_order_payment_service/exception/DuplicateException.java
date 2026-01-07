package com.nhnacademy.book2onandon_order_payment_service.exception;

public class DuplicateException extends RuntimeException {
    public DuplicateException(String message) {
        super(message);
    }
}
