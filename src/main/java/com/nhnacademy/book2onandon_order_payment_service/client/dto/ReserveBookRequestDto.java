package com.nhnacademy.book2onandon_order_payment_service.client.dto;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import java.util.List;

public record ReserveBookRequestDto(String orderNumber, List<BookInfoDto> bookInfoDtoList) {
}
