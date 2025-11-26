package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request;

public record TossResponse(String orderId,
                           Integer totalAmount,
                           String method,
                           String status,
                           String requestedAt,
                           Receipt receipt,
                           String paymentKey
) {
    public record Receipt(String url){}
}
