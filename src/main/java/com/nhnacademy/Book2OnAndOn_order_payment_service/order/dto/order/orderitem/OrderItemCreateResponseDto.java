package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;

public record OrderItemCreateResponseDto(Long orderItemId,
                                         Long bookId,
                                         Integer orderItemQuantity,
                                         Integer unitPrice,
                                         boolean isWrapped,
                                         OrderItemStatus orderItemStatus,
                                         Long wrappingPaperId) {
}
