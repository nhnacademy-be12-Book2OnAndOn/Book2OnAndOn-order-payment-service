package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonCancelResponse;
import java.util.List;

public record TossCancelResponse (String paymentKey,
                                  String status,
                                  List<Cancel> cancels){

    public CommonCancelResponse toCommonCancelResponse(){
        return new CommonCancelResponse(
                this.paymentKey,
                this.status,
                this.cancels
        );
    }
}
