package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

public class NotFoundPaymentException extends RuntimeException {
    public NotFoundPaymentException(String message) {
        super(message);
    }
}
