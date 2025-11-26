package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request;

import java.time.LocalDateTime;

public record PaymentCreateRequest(Long orderId,
                                   Integer totalAmount,
                                   String paymentMethod,
                                   String paymentProvider,
                                   String paymentStatus,
                                   LocalDateTime paymentDatetime,
                                   String paymentReceiptUrl,
                                   String paymentKey,
                                   Integer refundAmount) {
}
