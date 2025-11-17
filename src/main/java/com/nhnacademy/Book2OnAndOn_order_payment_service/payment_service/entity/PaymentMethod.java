package com.nhnacademy.Book2OnAndOn_order_payment_service.payment_service.entity;

public enum PaymentMethod {
    CARD,           // 신용/체크카드
    BANK_TRANSFER,  // 계좌 이체
    VIRTUAL_ACCOUNT,// 가상 계좌
    EASY_PAY,       // 간편 결제 (네이버페이 등)
    POINT           // 포인트 사용
}