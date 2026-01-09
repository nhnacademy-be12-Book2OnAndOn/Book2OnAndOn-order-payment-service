package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request;

public record PaymentUpdateRefundAmountRequest(String orderNumber, String paymentKey) {
}
