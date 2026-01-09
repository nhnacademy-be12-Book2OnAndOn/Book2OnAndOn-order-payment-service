package com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 포장지 목록 조회 시 사용(사용자 확인용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WrappingPaperSimpleResponseDto {
    private Long wrappingPaperId;
    private String wrappingPaperName;
    private int wrappingPaperPrice;
}