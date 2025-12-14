package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

import java.util.List;

public record CouponTargetResponseDto(Long memberCouponId,
                                      List<Long> targetBookIds,
                                      List<Long> targetCategoryIds,
                                      Integer minPrice,
                                      Integer maxPrice,
                                      String discountType,
                                      Integer discountValue) {
}
