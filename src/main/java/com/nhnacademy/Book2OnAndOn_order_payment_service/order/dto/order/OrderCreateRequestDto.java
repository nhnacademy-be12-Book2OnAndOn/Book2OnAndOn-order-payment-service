package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * [회원 주문 생성 요청 DTO] POST /api/orders
 * 회원 ID를 포함하며, 비회원 주문자 정보는 사용하지 않습니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {
    private Long userId;
    private List<OrderItemRequestDto> orderItems;
    private DeliveryAddressRequestDto deliveryAddress;

    // 쿠폰 , 포인트 금액
    private int couponDiscountAmount;
    private int pointDiscountAmount;
}