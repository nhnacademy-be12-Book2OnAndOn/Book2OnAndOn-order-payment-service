package com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity;

// 장바구니에 담긴 책 상태
public enum CartItemUnavailableReason {
    OUT_OF_STOCK,    // 재고 부족
    SALE_ENDED,      // 판매 종료
    BOOK_DELETED,    // 삭제된 도서
    BOOK_HIDDEN,     // 숨김 처리된 도서(검색 불가 등)
    INVALID_BOOK,    // 잘못된 도서 ID
    UNKNOWN          // 알 수 없음 (fallback)
}
