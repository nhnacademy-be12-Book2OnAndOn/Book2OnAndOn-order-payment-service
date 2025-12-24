package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception;

// 동시성 충돌
public class CartConcurrencyException extends CartException {

    public CartConcurrencyException(CartErrorCode errorCode) {
        super(errorCode);
    }

    public CartConcurrencyException(CartErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
