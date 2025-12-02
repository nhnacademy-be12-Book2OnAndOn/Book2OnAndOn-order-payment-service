package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto;

public record CommonConfirmRequest(String orderId,
                                   String paymentKey,
                                   Integer amount) {
}
