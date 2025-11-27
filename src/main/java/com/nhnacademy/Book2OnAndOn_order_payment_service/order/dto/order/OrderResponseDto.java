package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [공통 응답 DTO] 주문 생성, 상세 조회, 취소 등 모든 API의 응답 본문으로 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    // 1. 주문 기본 정보
    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private LocalDateTime orderDatetime;

    // 2. 금액 상세 정보 (Order 엔티티)
    private int totalAmount;
    private int totalDiscountAmount; 
    private int deliveryFee;
    
    // 3. 주문 상품 목록 (상세 조회용)
    private List<OrderItemDetailDto> orderItems;

    // 4. 배송지 정보
    private DeliveryAddressRequestDto deliveryAddress;
    
    // TODO: 결제 정보, 반품 정보 등 추가 필요
}