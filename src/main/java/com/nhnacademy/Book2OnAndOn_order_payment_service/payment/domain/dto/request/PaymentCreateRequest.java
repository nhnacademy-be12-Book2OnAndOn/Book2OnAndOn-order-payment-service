package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request;

import java.time.LocalDateTime;

// 확장성을 고려하여 결제 db 생성용 공통 DTO
public record PaymentCreateRequest(String paymentKey,
                                   String orderNumber,
                                   Integer totalAmount,
                                   String paymentMethod,
                                   String paymentProvider,
                                   String paymentStatus,
                                   LocalDateTime paymentCreatedAt,
                                   String paymentReceiptUrl,
                                   Integer refundAmount) {
}
