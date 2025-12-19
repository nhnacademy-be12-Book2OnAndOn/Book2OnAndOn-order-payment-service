package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
import java.time.LocalDateTime;

public record TossResponse(String orderId,
                           Integer totalAmount,
                           String method,
                           String status,
                           @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
                                  LocalDateTime requestedAt,
                           Receipt receipt,
                           String paymentKey
) {
    public record Receipt(String url){}

    // 공용 응답 변환
    public CommonResponse toCommonConfirmResponse(){
        return new CommonResponse(
                this.paymentKey,
                this.orderId,
                this.totalAmount,
                this.method,
                this.status,
                this.requestedAt,
                this.receipt.url
        );
    }
}
