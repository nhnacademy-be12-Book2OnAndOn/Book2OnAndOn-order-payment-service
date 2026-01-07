package com.nhnacademy.book2onandon_order_payment_service.client.dto;

import java.util.List;

public record OrderCouponCheckRequestDto(List<Long> bookIds, List<Long> categoryIds) {
}
