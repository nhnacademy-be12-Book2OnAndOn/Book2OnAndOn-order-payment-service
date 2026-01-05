package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;

public record OrderItemStatusUpdateDto(Long orderItemId, OrderItemStatus orderItemStatus) {
}
