package com.nhnacademy.book2onandon_order_payment_service.order.exception;

public class OrderNumberProvisionException extends RuntimeException {

    public OrderNumberProvisionException(String message) {
        super(message);
    }

    public OrderNumberProvisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
