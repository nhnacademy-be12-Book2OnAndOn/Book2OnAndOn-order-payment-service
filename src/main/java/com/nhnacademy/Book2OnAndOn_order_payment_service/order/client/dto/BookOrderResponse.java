package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// BookOrderResponse DTO (Book Service로부터 받을 응답)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookOrderResponse {
    private Long bookId;
    private String title;
    private int priceSales;
    private Integer stockCount;
    // ... (나머지 필드 생략)
}
