package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request;

import java.time.LocalDateTime;

// 생성
public record PaymentCancelCreateRequest(Integer cancelAmount,
                                   String cancelReason,
                                   LocalDateTime canceledAt,
                                   String paymentId) {
}
