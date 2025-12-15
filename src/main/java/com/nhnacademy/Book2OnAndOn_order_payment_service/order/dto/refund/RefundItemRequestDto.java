package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 반품할 개별 주문 항목 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundItemRequestDto {
    @NotNull
    private Long orderItemId; // 원 주문 항목 (OrderItem) ID

    @Min(1)
    private int refundQuantity; // 반품 수량
}