package com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto;

public record PointUsedRequestDto(String orderNumber, Long userId, Integer point) {}