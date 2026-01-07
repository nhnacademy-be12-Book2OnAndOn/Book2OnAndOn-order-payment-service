package com.nhnacademy.book2onandon_order_payment_service.order.exception;

/**
 * WrappingPaper 엔티티를 찾을 수 없을 때 발생하는 예외
 */
public class WrappingPaperNotFoundException extends RuntimeException {
    public WrappingPaperNotFoundException() {
        super("포장지 정보를 찾을 수 없습니다.");
    }

    public WrappingPaperNotFoundException(Long wrappingPaperId) {
        super("포장지 정보를 찾을 수 없습니다. (ID: " + wrappingPaperId + ")");
    }
}