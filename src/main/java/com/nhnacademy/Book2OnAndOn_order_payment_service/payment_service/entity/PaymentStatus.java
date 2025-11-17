package com.nhnacademy.Book2OnAndOn_order_payment_service.payment_service.entity;

public enum PaymentStatus {
    READY,          // 결제 대기
    APPROVED,       // 결제 승인 완료
    CANCELED,       // 결제 취소
    PARTIAL_CANCELED, // 부분 취소
    FAILED,         // 결제 실패
    REFUND_COMPLETED// 환불 완료
}