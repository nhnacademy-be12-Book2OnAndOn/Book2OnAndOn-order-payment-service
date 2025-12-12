package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

public record OrderItemDto(Long bookId, Integer quantity, Boolean isWrapped, Long wrappingPaperId){}