package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api.Cancel;

import java.util.List;

// 생성
public record PaymentCancelCreateRequest(String paymentKey,
                                         String status,
                                         List<Cancel> cancels) {
}
