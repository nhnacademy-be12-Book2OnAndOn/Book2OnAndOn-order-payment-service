package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import java.util.List;

public record CommonCancelResponse(String paymentKey,
                                   String status,
                                   List<Cancel> cancels) {

    public PaymentCancelCreateRequest toPaymentCancelCreateRequest(){
        return new PaymentCancelCreateRequest(
                this.paymentKey,
                this.status,
                this.cancels
        );
    }
}
