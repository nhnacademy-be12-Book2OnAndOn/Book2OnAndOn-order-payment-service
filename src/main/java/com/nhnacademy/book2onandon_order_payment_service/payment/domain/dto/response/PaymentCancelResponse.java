package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response;

import java.time.LocalDateTime;

public record PaymentCancelResponse(String paymentKey,
                                    Integer cancelAmount,
                                    String cancelReason,
                                    LocalDateTime canceledAt) {
}
