package com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundPointInternalRequestDto {
    private Long orderId;
    private Long refundId;
    private Integer usedPoint;      // 포인트 결제분 복구, nullable 가능
    private Integer refundPayPoint;   // 현금 결제분을 포인트로 적립, nullable 가능
}
