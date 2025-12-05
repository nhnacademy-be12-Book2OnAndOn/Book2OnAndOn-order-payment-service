package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService2 {
    // 유저 전용
    OrderSimpleDto createPreOrder(Long userId);
    Page<OrderSimpleDto> getOrderList(Long userId, Pageable pageable);
    OrderResponseDto getOrderDetail(Long userId, String orderNumber);

    // API 전용
    Boolean existsPurchase(Long userId, Long bookId);
}
