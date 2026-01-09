package com.nhnacademy.book2onandon_order_payment_service.client.dto;

import java.time.LocalDateTime;

public record OrderCanceledEvent(Long userId, Long orderId, Integer usedPoint, LocalDateTime occurredAt) {
}
