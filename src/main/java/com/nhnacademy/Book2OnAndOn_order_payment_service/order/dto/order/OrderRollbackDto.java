package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

public record OrderRollbackDto(String orderNumber, Long memberCouponId) {
}
