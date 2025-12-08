package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * POST /api/orders
 * 회원 ID를 포함하며, 비회원 주문자 정보는 사용 x
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {
    private List<OrderItemRequestDto> orderItems;
    private DeliveryAddressRequestDto deliveryAddress;
    private List<Long> couponId;
    private Long point;
}