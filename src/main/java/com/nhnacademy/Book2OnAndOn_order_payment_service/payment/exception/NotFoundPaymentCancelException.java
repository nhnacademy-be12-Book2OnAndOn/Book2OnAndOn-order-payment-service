package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

public class NotFoundPaymentCancelException extends NotFoundException {
    public NotFoundPaymentCancelException(String message) {
        super(message);
    }
}
