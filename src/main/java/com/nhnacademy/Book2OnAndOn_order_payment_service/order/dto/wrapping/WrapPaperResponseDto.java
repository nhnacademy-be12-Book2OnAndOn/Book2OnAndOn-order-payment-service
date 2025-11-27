package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * [포장지 상세 응답 DTO] 포장지 생성, 조회, 수정 시 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WrapPaperResponseDto {
    private Long wrappingPaperId;
    private String wrappingPaperName;
    private int wrappingPaperPrice;
    private String wrappingPaperPath;
}