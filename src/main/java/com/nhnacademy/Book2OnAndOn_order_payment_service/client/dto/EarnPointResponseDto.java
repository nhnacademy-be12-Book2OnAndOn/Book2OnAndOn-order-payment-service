package com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EarnPointResponseDto {
    private int changedPoint;
    private int totalPointAfter;
    private PointReason earnReason;    // SIGNUP / REVIEW / ORDER / REFUND / ADMIN_ADJUST 등
}

