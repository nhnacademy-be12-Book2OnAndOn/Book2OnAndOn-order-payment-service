package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService2 {
    // 유저 전용
    OrderSimpleDto createOrder(Long userId, OrderCreateRequestDto req);
    Page<OrderSimpleDto> getOrderList(Long userId, Pageable pageable);
    OrderResponseDto getOrderDetail(Long userId, String orderNumber);

    // 스케줄러 전용
    List<Long> findNextBatch(LocalDateTime thresholdTime, Long lastId, int batchSize);
    int deleteJunkOrder(List<Long> ids);

    // API 전용
    Boolean existsPurchase(Long userId, Long bookId);
}
