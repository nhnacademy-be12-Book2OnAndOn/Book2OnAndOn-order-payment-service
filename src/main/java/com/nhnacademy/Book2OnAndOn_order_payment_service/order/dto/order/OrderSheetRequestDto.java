package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import java.util.List;

public record OrderSheetRequestDto(List<BookInfoDto> bookItems) {
    public record BookInfoDto(Long bookId, Integer quantity){}
}
