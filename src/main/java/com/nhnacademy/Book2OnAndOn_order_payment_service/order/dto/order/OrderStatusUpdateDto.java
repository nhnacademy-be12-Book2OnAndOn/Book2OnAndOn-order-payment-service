package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * [관리자 DTO] 주문 상태 변경 요청 시 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateDto {
    private int statusCode;
    
    // 이 DTO는 Service에서 OrderStatus.fromCode(statusCode)로 변환하여 사용됩니다.
}