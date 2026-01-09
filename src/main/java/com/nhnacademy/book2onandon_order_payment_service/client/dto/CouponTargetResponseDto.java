package com.nhnacademy.book2onandon_order_payment_service.client.dto;

import java.util.List;

public record CouponTargetResponseDto(Long memberCouponId,
                                      List<Long> targetBookIds,
                                      List<Long> targetCategoryIds,
                                      Integer minPrice,
                                      Integer maxPrice,
                                      CouponPolicyDiscountType discountType,
                                      Integer discountValue) {
}
