package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response;

import java.time.LocalDateTime;

public record PaymentCancelResponse(Long paymentCancelId,
                                    Long paymentId,
                                    Integer cancelAmount,
                                    String cancelReason,
                                    LocalDateTime canceledAt,
                                    String paymentTransactionKey) {
}
