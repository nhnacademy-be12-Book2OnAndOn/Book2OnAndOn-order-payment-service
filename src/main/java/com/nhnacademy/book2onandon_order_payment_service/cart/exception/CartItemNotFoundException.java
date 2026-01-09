package com.nhnacademy.book2onandon_order_payment_service.cart.exception;

// 404
public class CartItemNotFoundException extends CartException {

    public CartItemNotFoundException(CartErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
