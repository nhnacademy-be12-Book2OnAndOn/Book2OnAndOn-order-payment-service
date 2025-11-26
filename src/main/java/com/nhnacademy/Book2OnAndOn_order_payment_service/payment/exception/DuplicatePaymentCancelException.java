package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

public class DuplicatePaymentCancelException extends RuntimeException {
    public DuplicatePaymentCancelException(String message) {
        super(message);
    }
}
