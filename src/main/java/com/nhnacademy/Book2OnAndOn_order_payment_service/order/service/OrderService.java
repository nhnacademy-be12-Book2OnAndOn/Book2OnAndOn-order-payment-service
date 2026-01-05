package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderStatusUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestOrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemStatusUpdateDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    // 회원 전용
    OrderPrepareResponseDto prepareOrder(Long userId, OrderPrepareRequestDto req);
    OrderCreateResponseDto createPreOrder(Long userId, String guestId, OrderCreateRequestDto req);
    Page<OrderSimpleDto> getOrderList(Long userId, Pageable pageable);
    OrderDetailResponseDto getOrderDetail(Long userId, String orderNumber, String token);
    void cancelOrder(Long userId, String orderNumber);

    // 비회원 전용
    OrderPrepareResponseDto prepareGuestOrder(String guestId, OrderPrepareRequestDto req);

    void cancelGuestOrder(String orderNumber, String guestToken);

    OrderCreateResponseDto createGuestPreOrder(String guestId, GuestOrderCreateRequestDto req);

    // 관리자 전용
    Page<OrderSimpleDto> getOrderListWithAdmin(Pageable pageable);
    OrderDetailResponseDto getOrderDetailWithAdmin(String orderNumber);
    void cancelOrderByAdmin(String orderNumber);

    void setOrderStatus(String orderNumber, OrderStatusUpdateDto req);
    void setOrderItemStatus(String orderNumber, OrderItemStatusUpdateDto req);

    // 스케줄러 전용
    List<Long> findNextBatch(LocalDateTime thresholdTime, Long lastId, int batchSize);
    int deleteJunkOrder(List<Long> ids);
}
