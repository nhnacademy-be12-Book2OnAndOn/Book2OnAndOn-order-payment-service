package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 반품 완료 시 결제금액만큼 포인트 반환
 * 내부 통신 DTO
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefundPointRequestDto {
    Long userId;
    Long orderId;
    int refundAmount;
}