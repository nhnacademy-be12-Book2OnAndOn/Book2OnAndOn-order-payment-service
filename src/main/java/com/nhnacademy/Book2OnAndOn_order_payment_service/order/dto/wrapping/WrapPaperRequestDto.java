package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 *  포장지 생성 및 수정 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WrapPaperRequestDto {
    private String wrappingPaperName;
    private int wrappingPaperPrice;
    private String wrappingPaperPath; // 이미지 경로
}