package com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// BookOrderResponse DTO (Book Service로부터 받을 응답)
@Getter
@NoArgsConstructor
public class BookOrderResponse {
    private Long bookId;
    private String title;
    private Long priceStandard;
    private Long priceSales;
    private String imageUrl;
    private boolean isPackable;
    private Integer stockCount;
    private String stockStatus;
    private Long categoryId;
    private Integer quantity;

    public void setQuantity(Integer quantity){
        this.quantity = quantity;
    }
}