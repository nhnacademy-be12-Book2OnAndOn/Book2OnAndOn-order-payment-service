package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.NotSupportedException;

public class NotSupportedPayments extends NotSupportedException {
    public NotSupportedPayments(String message) {
        super(message);
    }
}
