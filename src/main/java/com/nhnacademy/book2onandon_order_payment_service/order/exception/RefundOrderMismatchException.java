package com.nhnacademy.book2onandon_order_payment_service.order.exception;

public class RefundOrderMismatchException extends RuntimeException {

    private static final String MESSAGE =
            "orderId가 반품 내역의 주문과 일치하지 않습니다. orderId=%s, refundId=%s, refund.orderId=%s";

    public RefundOrderMismatchException(Long orderId, Long refundId, Long refundOrderId) {
        super(String.format(MESSAGE, orderId, refundId, refundOrderId));
    }
}
