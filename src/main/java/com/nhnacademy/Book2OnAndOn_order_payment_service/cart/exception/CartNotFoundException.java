package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception;

// 404
public class CartNotFoundException extends CartException {

    public CartNotFoundException(CartErrorCode errorCode) {
        super(errorCode);
    }

    public CartNotFoundException(CartErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
