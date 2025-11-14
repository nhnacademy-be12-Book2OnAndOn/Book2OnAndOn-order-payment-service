package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity;

/**
 * 반품 사유를 정의하는 Enum입니다.
 * ERD의 Return 테이블에 사용되는 returnReason 필드에 대응됩니다.
 */
public enum ReturnReason {
    CHANGE_OF_MIND,  //단순 변심
    PRODUCT_DEFECT,  // 상품 불량
    WRONG_DELIVERY,  // 배송 문제
    OTHER    // 기타
}