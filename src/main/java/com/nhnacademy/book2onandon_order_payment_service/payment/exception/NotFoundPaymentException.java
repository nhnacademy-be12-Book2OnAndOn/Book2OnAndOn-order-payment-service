package com.nhnacademy.book2onandon_order_payment_service.payment.exception;

import com.nhnacademy.book2onandon_order_payment_service.exception.NotFoundException;

public class NotFoundPaymentException extends NotFoundException {
    public NotFoundPaymentException(String message) {
        super(message);
    }
}
