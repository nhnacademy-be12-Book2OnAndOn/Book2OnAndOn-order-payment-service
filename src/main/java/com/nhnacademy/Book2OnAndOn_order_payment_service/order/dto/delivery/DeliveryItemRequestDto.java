package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 분할 배송 시 어떤 OrderItem이 몇 개 나가는지 명시
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryItemRequestDto {
    private Long orderItemId;
    private int quantity;
}