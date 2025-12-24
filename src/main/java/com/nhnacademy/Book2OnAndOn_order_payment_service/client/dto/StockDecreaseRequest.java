package com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// StockDecreaseRequest DTO (Book Service로 보낼 요청)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDecreaseRequest {
    private Long bookId;
    private Integer quantity;
}
