package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
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
public class GuestOrderCreateRequestDto {
    // 비회원 식별용 계정 및 주문 조회시 필요 데이터
    private String orderNumber;
    private String guestName;
    private String guestPhoneNumber;
    private String guestPassword;

    // 비회원 임시 주문용 데이터
    private List<OrderItemRequestDto> orderItems;
    private DeliveryAddressRequestDto deliveryAddress;
    @NotNull(message = "배송방법을 선택해주세요")
    private Long deliveryPolicyId; // 배송 방법 아이디
    @NotNull(message = "원하는 배송날짜를 선택해주세요")
    private LocalDate wantDeliveryDate;

}