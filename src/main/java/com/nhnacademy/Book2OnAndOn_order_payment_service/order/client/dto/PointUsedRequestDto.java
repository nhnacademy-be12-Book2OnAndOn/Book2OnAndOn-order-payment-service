package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

public record PointUsedRequestDto(String orderNumber, Long userId, Integer point) {}