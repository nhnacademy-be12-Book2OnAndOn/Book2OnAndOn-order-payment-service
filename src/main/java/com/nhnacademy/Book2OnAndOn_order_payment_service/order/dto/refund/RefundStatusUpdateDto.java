package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 관리자 반품 상태 변경 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatusUpdateDto {
    private int statusCode;
}