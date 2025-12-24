package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

/**
 * 주문 생성 과정 중 상품 가격 불일치, 재고 부족 등 유효성 검증에 실패했을 때 발생하는 예외
 */
public class OrderVerificationException extends RuntimeException {
    public OrderVerificationException(String message) {
        super(message);
    }
}