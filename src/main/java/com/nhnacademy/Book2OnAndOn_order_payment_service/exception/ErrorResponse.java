package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

public record ErrorResponse(String code, String message, int status) {
}
