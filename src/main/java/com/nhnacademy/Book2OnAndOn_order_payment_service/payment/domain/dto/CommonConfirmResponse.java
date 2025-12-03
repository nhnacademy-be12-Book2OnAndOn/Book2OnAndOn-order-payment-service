package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import java.time.LocalDateTime;

public record CommonConfirmResponse(String paymentKey,
                                    String orderId,
                                    Integer totalAmount,
                                    String method,
                                    String status,
                                    LocalDateTime requestedAt,
                                    String receiptUrl) {

    public PaymentCreateRequest toPaymentCreateRequest(String provider){
        return new PaymentCreateRequest(
                this.paymentKey,
                this.orderId,
                this.totalAmount,
                this.method,
                provider,
                this.status,
                this.requestedAt,
                this.receiptUrl,
                0
        );
    }
}
