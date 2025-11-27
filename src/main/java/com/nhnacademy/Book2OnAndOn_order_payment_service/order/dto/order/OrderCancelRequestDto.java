package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 회원 주문 취소 시 필요한 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelRequestDto {
    private String cancelReason;
    private String refundBankName;
    private String refundAccountNumber;
}