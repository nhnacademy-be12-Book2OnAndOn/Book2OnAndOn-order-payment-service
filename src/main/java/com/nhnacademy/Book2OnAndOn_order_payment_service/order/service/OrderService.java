package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    // 회원 전용
    OrderPrepareResponseDto prepareOrder(Long userId, OrderPrepareRequestDto req);
    OrderCreateResponseDto createPreOrder(Long userId, OrderCreateRequestDto req);
    Page<OrderSimpleDto> getOrderList(Long userId, Pageable pageable);
    OrderDetailResponseDto getOrderDetail(Long userId, String orderNumber);
    void cancelOrder(Long userId, String orderNumber);

    // 비회원 전용
    OrderPrepareResponseDto prepareGuestOrder(String guestId, OrderPrepareRequestDto req);

    // 스케줄러 전용
    List<Long> findNextBatch(LocalDateTime thresholdTime, Long lastId, int batchSize);
    int deleteJunkOrder(List<Long> ids);

    // -- 결제 --


    Boolean existsOrderByUserIdAndOrderNumber(Long userId, String orderNumber);
    Integer findTotalAmoundByOrderNumber(String orderNumber);
}
