package com.nhnacademy.book2onandon_order_payment_service.order.dto.order;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 *  주문 상태 변경 요청 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateDto {
    private OrderStatus orderStatus;
}