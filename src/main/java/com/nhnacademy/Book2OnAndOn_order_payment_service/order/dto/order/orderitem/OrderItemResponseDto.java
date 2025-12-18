package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponseDto {
    private Long orderItemId;
    private Long bookId;
    private String bookTitle;
    private String bookImageUrl;
    private Integer orderItemQuantity;
    private Integer unitPrice;
    private boolean isWrapped;
    private String orderItemStatus;
    private Long wrappingPaperId;
}
