package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response;

import java.time.LocalDateTime;

public record PaymentResponseDto(Long paymentId,
                                 Long orderId,
                                 Integer totalAmount,
                                 String paymentMethod,
                                 String paymentProvider,
                                 Byte paymentStatus,
                                 LocalDateTime paymentDatetime,
                                 String paymentReceiptUrl,
                                 String paymentKey,
                                 Integer refundAmount) {
}
