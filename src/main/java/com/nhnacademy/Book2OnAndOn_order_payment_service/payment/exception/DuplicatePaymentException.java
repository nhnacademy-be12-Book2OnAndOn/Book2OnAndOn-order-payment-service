package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

public class DuplicatePaymentException extends DuplicateException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}
