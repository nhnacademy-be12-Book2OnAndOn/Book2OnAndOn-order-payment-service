package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 포장지 수정 요청 시 필요한 데이터
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WrappingPaperUpdateRequestDto {
    private String wrappingPaperName;
    private int wrappingPaperPrice;
    private String wrappingPaperPath;
}