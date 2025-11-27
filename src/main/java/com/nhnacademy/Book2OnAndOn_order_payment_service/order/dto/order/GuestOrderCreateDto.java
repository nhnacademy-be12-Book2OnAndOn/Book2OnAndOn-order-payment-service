package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * POST /api/guest/orders
 * 비회원 주문자 정보와 주문 상품 목록을 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuestOrderCreateDto {
    private String guestName;
    private String guestPhoneNumber;
    private String guestPassword;
    private List<OrderItemRequestDto> orderItems;
    private DeliveryAddressRequestDto deliveryAddress;

    // 쿠폰 , 포인트
    private int couponDiscountAmount;
    private int pointDiscountAmount;
}