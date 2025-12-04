package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1;

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
public class ReturnItemRequestDto {
    @NotNull
    private Long orderItemId; // 원 주문 항목 (OrderItem) ID

    @Min(1)
    private int returnQuantity; // 반품 수량
}