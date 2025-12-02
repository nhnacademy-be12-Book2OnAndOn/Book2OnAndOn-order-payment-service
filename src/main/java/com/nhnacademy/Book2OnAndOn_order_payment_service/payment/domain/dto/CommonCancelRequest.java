package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto;

public record CommonCancelRequest(String paymentKey,
                                  Integer cancelAmount,
                                  String cancelReason) {
}
