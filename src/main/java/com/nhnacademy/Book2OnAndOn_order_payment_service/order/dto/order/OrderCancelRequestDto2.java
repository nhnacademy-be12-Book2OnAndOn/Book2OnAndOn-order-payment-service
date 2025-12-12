package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

// 주문 취소는 부분 취소없이 전체 취소만 가능
public record OrderCancelRequestDto2(String cancelReason) {
}
