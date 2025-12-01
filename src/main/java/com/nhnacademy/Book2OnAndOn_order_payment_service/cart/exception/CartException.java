package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception;

import lombok.Getter;

// 추상 상위 예외
@Getter
public abstract class CartException extends RuntimeException {

    private final CartErrorCode errorCode;

    protected CartException(CartErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    } // 표준 메시지 : 사용자에게 보여줄 기본 메시지

    protected CartException(CartErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    } // 상세 메시지 : 상황별로 조정된 상세 메시지 (로그/디버깅용)
}
