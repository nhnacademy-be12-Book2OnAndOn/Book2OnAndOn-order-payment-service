package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception;

// 비즈니스 로직 오류
public class CartBusinessException extends CartException {

    public CartBusinessException(CartErrorCode errorCode) {
        super(errorCode);
    }

    public CartBusinessException(CartErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
