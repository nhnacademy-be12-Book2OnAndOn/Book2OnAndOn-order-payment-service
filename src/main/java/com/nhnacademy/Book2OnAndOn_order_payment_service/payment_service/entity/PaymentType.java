package com.nhnacademy.Book2OnAndOn_order_payment_service.payment_service.entity;

public enum PaymentType {
    NORMAL,         // 일반 결제 (최초 결제)
    BILLING,        // 정기 결제
    SUBSCRIPTION    // 구독 결제
}