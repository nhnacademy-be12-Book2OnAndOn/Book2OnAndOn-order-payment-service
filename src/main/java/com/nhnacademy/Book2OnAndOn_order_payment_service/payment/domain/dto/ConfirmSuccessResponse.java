package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record ConfirmSuccessResponse(String paymentKey,
                                     String orderId,
                                     Integer totalAmount,
                                     String method,
                                     String status,
                                     LocalDateTime requestedAt,
                                     String receiptUrl) {
}
