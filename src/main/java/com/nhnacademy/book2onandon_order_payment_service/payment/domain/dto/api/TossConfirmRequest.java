package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api;

public record TossConfirmRequest(String orderId,
                                 String paymentKey,
                                 Integer amount) {
}
