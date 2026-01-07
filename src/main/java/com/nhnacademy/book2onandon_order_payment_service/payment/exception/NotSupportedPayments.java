package com.nhnacademy.book2onandon_order_payment_service.payment.exception;

import com.nhnacademy.book2onandon_order_payment_service.exception.NotSupportedException;

public class NotSupportedPayments extends NotSupportedException {
    public NotSupportedPayments(String message) {
        super(message);
    }
}
