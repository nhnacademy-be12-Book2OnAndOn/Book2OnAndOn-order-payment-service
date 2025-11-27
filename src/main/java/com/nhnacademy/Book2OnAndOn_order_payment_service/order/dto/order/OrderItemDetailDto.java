package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 주문 상세 조회 응답(OrderResponseDto)에 포함되는 개별 상품 상세 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDetailDto {
    private Long orderItemId;
    private String bookTitle; // 도서 모듈에서 조회해야 함
    private int quantity;
    private int unitPrice;
    private int totalPrice; // quantity * unitPrice
    private String wrappingPaperName;
    private String itemStatus; // OrderItemStatus.getDescription() 값
}