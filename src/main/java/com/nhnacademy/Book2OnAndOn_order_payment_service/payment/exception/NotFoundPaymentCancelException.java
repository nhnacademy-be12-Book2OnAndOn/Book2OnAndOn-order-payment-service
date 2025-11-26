package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

public class NotFoundPaymentCancelException extends RuntimeException {
    public NotFoundPaymentCancelException(String message) {
        super(message);
    }
}
