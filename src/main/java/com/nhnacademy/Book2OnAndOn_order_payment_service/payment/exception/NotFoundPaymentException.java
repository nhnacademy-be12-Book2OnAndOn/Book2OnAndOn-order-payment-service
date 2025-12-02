package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.NotFoundException;

public class NotFoundPaymentException extends NotFoundException {
    public NotFoundPaymentException(String message) {
        super(message);
    }
}
