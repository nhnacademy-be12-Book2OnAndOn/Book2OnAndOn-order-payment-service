package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;

import java.time.LocalDateTime;

/**
 * 주문 목록 페이지에서 핵심 요약 정보만 제공
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderSimpleDto {
    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private LocalDateTime orderDatetime;
    private int totalAmount; // 최종 결제 금액
    /** 대표 상품명  */
    private String representativeBookTitle; 
}