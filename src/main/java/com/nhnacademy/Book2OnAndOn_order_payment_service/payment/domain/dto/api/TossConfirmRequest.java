package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api;

public record TossConfirmRequest(Integer amount,
                                 String orderId,
                                 String paymentKey) {
}
