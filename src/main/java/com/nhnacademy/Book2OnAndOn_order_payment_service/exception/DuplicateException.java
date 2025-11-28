package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

public class DuplicateException extends RuntimeException {
    public DuplicateException(String message) {
        super(message);
    }
}
