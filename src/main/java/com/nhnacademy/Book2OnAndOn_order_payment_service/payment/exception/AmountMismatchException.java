package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.PaymentException;

public class AmountMismatchException extends PaymentException {
    public AmountMismatchException(String message) {
        super(message);
    }
}
