package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 포장지 목록 조회 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WrapPaperSimpleResponseDto {
    private Long wrappingPaperId;
    private String wrappingPaperName;
    private int wrappingPaperPrice;
}