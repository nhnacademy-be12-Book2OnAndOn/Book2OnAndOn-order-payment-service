package com.nhnacademy.book2onandon_order_payment_service.order.dto.order;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDto {
    // 1. 주문 기본 정보
    private Long orderId;
    private String orderNumber;
    private String orderStatus;
    private LocalDateTime orderDatetime;

    // 2. 금액 상세 정보 (Order 엔티티)
    private int totalAmount;
    private int totalDiscountAmount;
    private int totalItemAmount;
    private int deliveryFee;
    private int wrappingFee;
    private int couponDiscount;
    private int pointDiscount;

    // 배송 희망 날짜
    private LocalDate wantDeliveryDate;

    // 3. 주문 상품 목록 (상세 조회용)
    private List<OrderItemResponseDto> orderItems;

    // 4. 배송지 정보
    private DeliveryAddressResponseDto deliveryAddress;

    // 5. 결제 정보
    private PaymentResponse paymentResponse;
}
