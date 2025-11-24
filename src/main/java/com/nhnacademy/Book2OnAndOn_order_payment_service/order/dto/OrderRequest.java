package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private Long userId; // 회원 고유 식별 아이디
    private GuestOrderRequest guestOrderInfo; // 비회원 주문자 정보
    private List<OrderItemRequest> orderItems; // 주문할 상품 목록
    private DeliveryAddressInfoRequest deliveryAddressInfo; // 배송지 정보

    /** 적용된 쿠폰/포인트 정보 (결제 금액 계산 및 할인을 위해 필요) */
    private Long usedCouponId;
    private int usedPointAmount;
}