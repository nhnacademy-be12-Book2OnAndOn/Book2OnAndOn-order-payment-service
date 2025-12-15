package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import java.util.List;

/**
 * 장바구니 혹은 바로 구매시 주문항목 리스트
 * @param bookItems
 */
public record OrderPrepareRequestDto(List<BookInfoDto> bookItems) {
    public record BookInfoDto(Long bookId, Integer quantity){}
}
