package com.nhnacademy.book2onandon_order_payment_service.order.dto.order;

import jakarta.validation.constraints.NotNull;

// 주문 취소는 부분 취소없이 전체 취소만 가능
public record OrderCancelRequestDto(
        @NotNull(message = "취소 사유를 입력해주세요")
        String cancelReason) {
}
