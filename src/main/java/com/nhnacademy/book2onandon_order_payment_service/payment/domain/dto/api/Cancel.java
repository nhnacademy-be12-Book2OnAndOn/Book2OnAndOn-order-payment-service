package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record Cancel(Integer cancelAmount,
                     String cancelReason,
                     @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
                     LocalDateTime canceledAt) {
}
