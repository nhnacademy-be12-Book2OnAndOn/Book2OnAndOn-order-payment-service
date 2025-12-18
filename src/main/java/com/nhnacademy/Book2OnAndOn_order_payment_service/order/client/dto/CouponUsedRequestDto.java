package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

public record CouponUsedRequestDto(String orderNumber, Long memberCouponId) {}