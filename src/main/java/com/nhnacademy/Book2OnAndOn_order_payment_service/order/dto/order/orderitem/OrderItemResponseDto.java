package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem;

public record OrderItemResponseDto(Long orderItemId,
                                   Long bookId,
                                   String bookTitle,
                                   String bookImagePath,
                                   Integer orderItemQuantity,
                                   Integer unitPrice,
                                   boolean isWrapped,
                                   String orderItemStatus,
                                   Long wrappingPaperId
                                   ) {
}
