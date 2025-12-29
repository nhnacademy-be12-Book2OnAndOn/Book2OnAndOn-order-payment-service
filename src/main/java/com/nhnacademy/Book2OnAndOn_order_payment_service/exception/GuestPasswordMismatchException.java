package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

public class GuestPasswordMismatchException extends RuntimeException {
    public GuestPasswordMismatchException(String message) {
        super(message);
    }
}
