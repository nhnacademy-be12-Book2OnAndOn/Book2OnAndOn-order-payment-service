package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId; // 주문 고유 식별자
    private String orderNumber; // 주문번호
    private int totalPaymentAmount; // 최종결제금
    private OrderStatus orderStatus; // 주문상태
}