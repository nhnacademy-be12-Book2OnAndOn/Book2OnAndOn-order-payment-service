package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity;
/**
 * 주문의 현재 상태
 */
public enum OrderStatus {
    PENDING,        // 대기
    COMPLETED,      // 완료
    SHIPPING,        // 배송 중
    DELIVERED,      // 배송 완료
    CANCELED,       // 주문 취소
    RETURNED,       // 반품 처리 중
    REFUNDED        // 환불 완료
}