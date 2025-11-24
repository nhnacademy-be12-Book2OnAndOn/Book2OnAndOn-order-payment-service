package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder; // 객체 생성을 위한 Builder 패턴 추가

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // 복잡한 객체 생성 시 유용
public class OrderItemRequest {
    private Long bookId;
    private int quantity;   // order_item_quantity
    private Long wrappingPaperId;
    private boolean isWrapped; // 포장유무
}