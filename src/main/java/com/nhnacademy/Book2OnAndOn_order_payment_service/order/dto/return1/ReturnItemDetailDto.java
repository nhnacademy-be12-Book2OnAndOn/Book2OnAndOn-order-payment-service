package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 반품 상세 조회 응답(ReturnResponseDto)에 포함되는 개별 반품 항목 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemDetailDto {
    private Long returnItemId;
    private Long orderItemId; // 원 주문 항목 ID
    private String bookTitle;
    private int returnQuantity;
}