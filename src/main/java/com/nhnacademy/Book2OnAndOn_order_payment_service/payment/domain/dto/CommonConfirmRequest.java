package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmRequest;

public record CommonConfirmRequest(String orderId,
                                   String paymentKey,
                                   Integer amount) {

    // 토스 응답 변환
    public TossConfirmRequest toTossConfirmRequest(){
        return new TossConfirmRequest(
                this.orderId,
                this.paymentKey,
                this.amount
        );
    }
}
