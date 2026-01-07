package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api;

public record TossCancelRequest(String cancelReason,
                                Integer cancelAmount) {
}
