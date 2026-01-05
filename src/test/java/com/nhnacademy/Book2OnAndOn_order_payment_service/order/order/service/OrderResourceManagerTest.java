package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.EarnOrderPointRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.OrderCanceledEvent;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.ReserveBookRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UseCouponRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UsePointInternalRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.config.RabbitConfig;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class OrderResourceManagerTest {

    @InjectMocks
    private OrderResourceManager orderResourceManager;

    @Mock private BookServiceClient bookServiceClient;
    @Mock private CouponServiceClient couponServiceClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("자원 선점 - 회원 (쿠폰/포인트 사용)")
    void prepareResources_Member_Full() {
        Long userId = 1L;
        Long orderId = 100L;
        Long couponId = 10L;
        int point = 1000;

        // Mock Items
        OrderItemRequestDto itemReq = new OrderItemRequestDto(1L, 2, false, null);
        OrderCreateRequestDto req = new OrderCreateRequestDto();
        req.setOrderItems(List.of(itemReq));
        req.setMemberCouponId(couponId);

        OrderVerificationResult result = new OrderVerificationResult(
                "ORD-001", "Title", 10000, 1000, 11000, 0, 0, 0, point, null, List.of(), null
        );

        orderResourceManager.prepareResources(userId, req, result, orderId);

        verify(bookServiceClient).reserveStock(any(ReserveBookRequestDto.class));
        verify(couponServiceClient).useCoupon(eq(couponId), eq(userId), any(UseCouponRequestDto.class));
        verify(userServiceClient).usePoint(eq(userId), any(UsePointInternalRequestDto.class));
    }

    @Test
    @DisplayName("자원 선점 - 회원 (쿠폰X, 포인트X)")
    void prepareResources_Member_NoCouponNoPoint() {
        Long userId = 1L;
        Long orderId = 100L;
        OrderCreateRequestDto req = new OrderCreateRequestDto();
        req.setOrderItems(List.of(new OrderItemRequestDto(1L, 1, false, null)));
        req.setMemberCouponId(null); // 쿠폰 없음

        OrderVerificationResult result = new OrderVerificationResult(
                "ORD-001", "Title", 10000, 0, 10000, 0, 0, 0, 0, null, List.of(), null // 포인트 0
        );

        orderResourceManager.prepareResources(userId, req, result, orderId);

        verify(bookServiceClient).reserveStock(any());
        verify(couponServiceClient, never()).useCoupon(any(), any(), any());
        verify(userServiceClient, never()).usePoint(any(), any());
    }

    @Test
    @DisplayName("자원 선점 - 비회원")
    void prepareResources_Guest() {
        Long userId = null;
        Long orderId = 100L;
        OrderCreateRequestDto req = new OrderCreateRequestDto();
        req.setOrderItems(List.of(new OrderItemRequestDto(1L, 1, false, null)));

        OrderVerificationResult result = new OrderVerificationResult(
                "ORD-001", "Title", 10000, 0, 10000, 0, 0, 0, 0, null, List.of(), null
        );

        orderResourceManager.prepareResources(userId, req, result, orderId);

        verify(bookServiceClient).reserveStock(any());
        // 회원이 아니면 아래 로직 수행 안함
        verify(couponServiceClient, never()).useCoupon(any(), any(), any());
        verify(userServiceClient, never()).usePoint(any(), any());
    }

    @Test
    @DisplayName("자원 복구 - 회원 (포인트 사용)")
    void releaseResources_Member() {
        String orderNumber = "ORD-001";
        Long userId = 1L;
        Long orderId = 100L;
        int point = 500;

        orderResourceManager.releaseResources(orderNumber, userId, point, orderId);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_BOOK), eq(orderNumber));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_COUPON), eq(orderNumber));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_POINT), any(OrderCanceledEvent.class));
    }

    @Test
    @DisplayName("자원 복구 - 회원 (포인트 0)")
    void releaseResources_Member_NoPoint() {
        String orderNumber = "ORD-001";
        Long userId = 1L;
        Long orderId = 100L;
        int point = 0;

        orderResourceManager.releaseResources(orderNumber, userId, point, orderId);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_BOOK), eq(orderNumber));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_COUPON), eq(orderNumber));
        // 포인트 복구 메시지는 전송되지 않아야 함
        verify(rabbitTemplate, never()).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_POINT), any(OrderCanceledEvent.class));
    }

    @Test
    @DisplayName("자원 복구 - 비회원")
    void releaseResources_Guest() {
        String orderNumber = "ORD-001";
        Long userId = null;
        Long orderId = 100L;
        int point = 0;

        orderResourceManager.releaseResources(orderNumber, userId, point, orderId);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_BOOK), eq(orderNumber));
        // 비회원은 쿠폰/포인트 복구 로직 안 탐

    }

    @Test
    @DisplayName("주문 완료(확정) - 회원")
    void completeOrder_Member() {
        Long userId = 1L;
        String orderNumber = "ORD-001";
        Long orderId = 100L;
        int totalAmount = 5000;

        orderResourceManager.completeOrder(userId, orderNumber, orderId, totalAmount);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CONFIRM_BOOK), eq(orderNumber));
        verify(userServiceClient).earnOrderPoint(eq(userId), any(EarnOrderPointRequestDto.class));
    }

    @Test
    @DisplayName("주문 완료(확정) - 비회원")
    void completeOrder_Guest() {
        Long userId = null;
        String orderNumber = "ORD-001";
        Long orderId = 100L;
        int totalAmount = 5000;

        orderResourceManager.completeOrder(userId, orderNumber, orderId, totalAmount);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CONFIRM_BOOK), eq(orderNumber));
        verify(userServiceClient, never()).earnOrderPoint(any(), any());
    }

    @Test
    @DisplayName("포인트 롤백 직접 호출")
    void rollbackPoint() {
        orderResourceManager.rollbackPoint(100L, 1L, 1000);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_POINT), any(OrderCanceledEvent.class));
    }

    @Test
    @DisplayName("포인트 확정 - 포인트 0 이하")
    void confirmPoint_Zero() {
        orderResourceManager.confirmPoint(100L, 1L, 0);
        verify(userServiceClient, never()).usePoint(any(), any());
    }

    @Test
    @DisplayName("포인트 확정 - 포인트 null")
    void confirmPoint_Null() {
        orderResourceManager.confirmPoint(100L, 1L, null);
        verify(userServiceClient, never()).usePoint(any(), any());
    }
}