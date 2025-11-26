package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response;

import java.time.LocalDateTime;

public record PaymentResponse(Long paymentId,
                              String orderNumber,
                              Integer totalAmount,
                              String paymentMethod,
                              String paymentProvider,
                              String paymentStatus,
                              LocalDateTime paymentCreatedAt,
                              String paymentReceiptUrl,
                              String paymentKey,
                              Integer refundAmount) {
}
