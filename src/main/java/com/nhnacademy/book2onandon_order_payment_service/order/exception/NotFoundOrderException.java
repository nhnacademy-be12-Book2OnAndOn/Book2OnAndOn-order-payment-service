package com.nhnacademy.book2onandon_order_payment_service.order.exception;

import com.nhnacademy.book2onandon_order_payment_service.exception.NotFoundException;

public class NotFoundOrderException extends NotFoundException {
    public NotFoundOrderException(String message) {
        super(message);
    }
}
