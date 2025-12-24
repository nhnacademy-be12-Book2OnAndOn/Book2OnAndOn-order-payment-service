package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelRequest;

public record CommonCancelRequest(String paymentKey,
                                  Integer cancelAmount,
                                  String cancelReason) {

    public TossCancelRequest toTossCancelRequest(){
        return new TossCancelRequest(
                this.cancelReason,
                this.cancelAmount);
    }
}
