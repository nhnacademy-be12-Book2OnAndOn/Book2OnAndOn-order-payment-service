package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;

import java.util.List;

// 생성
public record PaymentCancelCreateRequest(String paymentKey,
                                         String status,
                                         List<Cancel> cancels) {
}
