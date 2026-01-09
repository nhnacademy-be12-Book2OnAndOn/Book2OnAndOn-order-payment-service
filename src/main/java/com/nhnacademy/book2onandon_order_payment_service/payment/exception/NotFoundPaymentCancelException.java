package com.nhnacademy.book2onandon_order_payment_service.payment.exception;

import com.nhnacademy.book2onandon_order_payment_service.exception.NotFoundException;

public class NotFoundPaymentCancelException extends NotFoundException {
    public NotFoundPaymentCancelException(String message) {
        super(message);
    }
}
