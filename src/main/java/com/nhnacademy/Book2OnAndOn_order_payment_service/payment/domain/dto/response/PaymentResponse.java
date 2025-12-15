package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(String paymentKey,
                              String orderNumber,
                              Integer totalAmount,
                              String paymentMethod,
                              String paymentProvider,
                              String paymentStatus,
                              LocalDateTime paymentCreatedAt,
                              String paymentReceiptUrl,
                              Integer refundAmount,
                              List<PaymentCancelResponse> paymentCancelResponseList) {
}
