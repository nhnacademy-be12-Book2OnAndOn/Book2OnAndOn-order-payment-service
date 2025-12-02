package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api;

public record TossCancelRequest(String cancelReason,
                                Integer cancelAmount) {
}
