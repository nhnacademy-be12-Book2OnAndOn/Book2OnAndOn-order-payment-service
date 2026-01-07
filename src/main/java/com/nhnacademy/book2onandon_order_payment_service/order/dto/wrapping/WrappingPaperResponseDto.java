package com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 포장지 생성, 조회, 수정 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WrappingPaperResponseDto {
    private Long wrappingPaperId;
    private String wrappingPaperName;
    private int wrappingPaperPrice;
    private String wrappingPaperPath;
}