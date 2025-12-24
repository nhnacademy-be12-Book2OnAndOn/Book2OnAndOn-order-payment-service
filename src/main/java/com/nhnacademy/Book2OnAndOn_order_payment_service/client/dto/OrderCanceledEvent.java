package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

import java.time.LocalDateTime;

public record OrderCanceledEvent(Long userId, Long orderId, Integer usedPoint, LocalDateTime occurredAt) {
}
