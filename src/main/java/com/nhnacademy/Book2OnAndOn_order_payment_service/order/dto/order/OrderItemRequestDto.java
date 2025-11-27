package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 개별 상품 주문 정보를 담는 DTO. OrderCreateRequestDto와 GuestOrderCreateDto에 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequestDto {
    private Long bookId;
    private int quantity;   // order_item_quantity
    private Long wrappingPaperId;
    private boolean isWrapped;
}