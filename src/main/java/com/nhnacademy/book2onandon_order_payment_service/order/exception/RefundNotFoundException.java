package com.nhnacademy.book2onandon_order_payment_service.order.exception;

public class RefundNotFoundException extends RuntimeException {

    public RefundNotFoundException(Long refundId) {
        super("반품 내역을 찾을 수 없습니다. id=" + refundId);
    }

    // 기존 코드 호환이 필요하면 유지
    public RefundNotFoundException(String message) {
        super(message);
    }
}