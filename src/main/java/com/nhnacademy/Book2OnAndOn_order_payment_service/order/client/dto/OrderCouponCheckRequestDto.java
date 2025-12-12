package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

import java.util.List;

public record OrderCouponCheckRequestDto(List<Long> bookIds, List<Long> categoryIds) {
}
