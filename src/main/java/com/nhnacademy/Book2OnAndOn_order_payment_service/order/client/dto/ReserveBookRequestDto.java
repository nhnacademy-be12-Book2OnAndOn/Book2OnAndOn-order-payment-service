package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import java.util.List;

public record ReserveBookRequestDto(Long orderId, List<BookInfoDto> bookInfoDtoList) {
}
