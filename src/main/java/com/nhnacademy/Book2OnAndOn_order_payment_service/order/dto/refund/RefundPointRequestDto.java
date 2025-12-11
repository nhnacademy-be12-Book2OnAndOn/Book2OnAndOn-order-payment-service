package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefundPointRequestDto {
    Long userId;
    Long orderId;
    int refundAmount;
}
