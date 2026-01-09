package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request;

// 조회
public record PaymentCancelRequest(String orderNumber, String reason, Integer amount) {
}
