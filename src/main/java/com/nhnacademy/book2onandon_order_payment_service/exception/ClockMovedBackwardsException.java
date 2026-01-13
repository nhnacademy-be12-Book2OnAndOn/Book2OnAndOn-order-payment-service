package com.nhnacademy.book2onandon_order_payment_service.exception;

public class ClockMovedBackwardsException extends RuntimeException {
    public ClockMovedBackwardsException(String message) {
        super(message);
    }

    public ClockMovedBackwardsException(String message, Throwable cause) {
        super(message, cause);
    }
}
