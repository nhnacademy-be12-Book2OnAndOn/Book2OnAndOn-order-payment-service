package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

import java.time.LocalDateTime;

public class MemberCouponResponseDto {
    private Long memberCouponId;
    private String couponName;
    private Integer minPrice;   // 최소 주문 금액
    private Integer maxPrice;   // 최대 할인 금액
    private Integer discountValue;
    private String discountType;       // Enum 대신 String으로 받아도 무방
    private String memberCouponStatus; // Enum 대신 String으로 받아도 무방
    private LocalDateTime memberCouponEndDate;
    private LocalDateTime memberCouponUseDate;
    private String discountDescription;
}