package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api;

public record TossCancelResponse (Cancels cancels){
    public record Cancels(Integer cancelAmount,
                          String cancelReason,
                          String canceledAt){}
}
