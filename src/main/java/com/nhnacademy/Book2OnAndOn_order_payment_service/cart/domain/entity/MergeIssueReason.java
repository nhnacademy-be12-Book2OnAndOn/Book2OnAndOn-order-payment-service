package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity;

public enum MergeIssueReason {
    EXCEEDED_MAX_QUANTITY,    // 최대 수량(예: 99) 초과 → cap 처리
    UNAVAILABLE,              // 품절/판매중지/삭제 등으로 구매 불가
    FAILED,                   // 기타 실패 (예: 내부 에러)
    DUPLICATE,                // 중복된 요청/이미 처리된 항목
    CART_SIZE_LIMIT_EXCEEDED, // 장바구니 최대 품목 수 초과
    STOCK_LIMIT_EXCEEDED,     // merge 시 재고초과
    UNKNOWN                   // 알 수 없음 (fallback)
}
