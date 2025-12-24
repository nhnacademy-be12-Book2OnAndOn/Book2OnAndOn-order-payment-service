package com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto;

public enum PointReason {
    SIGNUP, // 회원가입
    REVIEW, // 리뷰 작성 적립
    ORDER, // 도서 결제 적립 (등급 적립)
    USE, // 포인트 사용
    REFUND, // 주문 취소/반품으로 인한 반환
    EXPIRE, // 만료
    FAILED, // 결제 실패
    WITHDRAW, // 회원탈퇴
    ADMIN_ADJUST // 관리자 수동 지급/차감
}