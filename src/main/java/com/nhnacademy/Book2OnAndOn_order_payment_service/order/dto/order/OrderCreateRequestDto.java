package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
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
    @NotNull(message = "배송방법을 선택해주세요")
    private Long deliveryPolicyId; // 배송 방법 아이디
    @NotNull(message = "원하는 배송날짜를 선택해주세요")
    private LocalDate wantDeliveryDate;
    private Long couponId; // 하나의 주문에 하나의 쿠폰만 사용
    private Integer point;
}