package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund;

public enum RefundReason {
    CHANGE_OF_MIND,  //단순 변심
    PRODUCT_DEFECT,  // 상품 불량(파손/파본)
    WRONG_DELIVERY,  // 배송 문제
    OTHER    // 기타
}