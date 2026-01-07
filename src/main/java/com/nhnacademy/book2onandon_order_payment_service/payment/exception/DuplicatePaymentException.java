package com.nhnacademy.book2onandon_order_payment_service.payment.exception;

import com.nhnacademy.book2onandon_order_payment_service.exception.DuplicateException;

public class DuplicatePaymentException extends DuplicateException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}
