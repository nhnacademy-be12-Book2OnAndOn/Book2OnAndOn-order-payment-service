package com.nhnacademy.book2onandon_order_payment_service.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nhnacademy.book2onandon_order_payment_service.client.BookServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.CouponServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.UserServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.EarnOrderPointRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.OrderCanceledEvent;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.ReserveBookRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.UseCouponRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.UsePointInternalRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.config.RabbitConfig;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
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
    private OrderResourceManager resourceManager;

    @Mock private BookServiceClient bookServiceClient;
    @Mock private CouponServiceClient couponServiceClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("자원 준비 성공 - 회원 (도서 예약, 쿠폰 사용, 포인트 차감)")
    void prepareResources_Member_Success() {
        Long userId = 1L;
        Long orderId = 10L;
        Long couponId = 50L;
        String orderNumber = "ORD-001";

        OrderItemRequestDto item = new OrderItemRequestDto(100L, 2, false, null);
        OrderCreateRequestDto req = mock(OrderCreateRequestDto.class);
        OrderVerificationResult result = mock(OrderVerificationResult.class);

        when(req.getOrderItems()).thenReturn(List.of(item));
        when(req.getMemberCouponId()).thenReturn(couponId);
        when(result.orderNumber()).thenReturn(orderNumber);
        when(result.pointDiscount()).thenReturn(500);

        resourceManager.prepareResources(userId, req, result, orderId);

        verify(bookServiceClient, times(1)).reserveStock(any(ReserveBookRequestDto.class));
        verify(couponServiceClient, times(1)).useCoupon(eq(couponId), eq(userId), any(UseCouponRequestDto.class));
        verify(userServiceClient, times(1)).usePoint(eq(userId), any(UsePointInternalRequestDto.class));
    }

    @Test
    @DisplayName("자원 준비 - 비회원 (도서만 예약하고 종료)")
    void prepareResources_Guest_Success() {
        OrderItemRequestDto item = new OrderItemRequestDto(100L, 1, false, null);
        OrderCreateRequestDto req = mock(OrderCreateRequestDto.class);
        OrderVerificationResult result = mock(OrderVerificationResult.class);

        when(req.getOrderItems()).thenReturn(List.of(item));
        when(result.orderNumber()).thenReturn("GUEST-ORD");

        resourceManager.prepareResources(null, req, result, 10L);

        verify(bookServiceClient, times(1)).reserveStock(any(ReserveBookRequestDto.class));
        verifyNoInteractions(couponServiceClient, userServiceClient);
    }

    @Test
    @DisplayName("자원 복구 성공 - 회원 (메시지 큐 전송 및 포인트 롤백)")
    void releaseResources_Member_Success() {
        resourceManager.releaseResources("ORD-001", 1L, 1000, 10L);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_BOOK), eq("ORD-001"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_COUPON), eq("ORD-001"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_POINT), any(OrderCanceledEvent.class));
    }

    @Test
    @DisplayName("자원 복구 - 비회원 (도서만 복구)")
    void releaseResources_Guest_Success() {
        resourceManager.releaseResources("ORD-001", null, 0, 10L);

        verify(rabbitTemplate).convertAndSend(any(), eq(RabbitConfig.ROUTING_KEY_CANCEL_BOOK), anyString());
        verify(rabbitTemplate, never()).convertAndSend(any(), eq(RabbitConfig.ROUTING_KEY_CANCEL_COUPON), anyString());
    }

    @Test
    @DisplayName("주문 완료 처리 - 회원 (도서 확정 및 포인트 적립)")
    void completeOrder_Member_Success() {
        resourceManager.completeOrder(1L, "ORD-001", 10L, 50000);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CONFIRM_BOOK), eq("ORD-001"));
        verify(userServiceClient).earnOrderPoint(eq(1L), any(EarnOrderPointRequestDto.class));
    }

    @Test
    @DisplayName("주문 완료 처리 - 비회원 (도서 확정만 수행)")
    void completeOrder_Guest_Success() {
        resourceManager.completeOrder(null, "ORD-001", 10L, 50000);

        verify(rabbitTemplate).convertAndSend(any(), eq(RabbitConfig.ROUTING_KEY_CONFIRM_BOOK), anyString());
        verify(userServiceClient, never()).earnOrderPoint(anyLong(), any());
    }

    @Test
    @DisplayName("포인트 확정 - 포인트가 0이거나 null인 경우 호출 안함")
    void confirmPoint_Fail_InvalidPoint() {
        resourceManager.confirmPoint(10L, 1L, 0);
        resourceManager.confirmPoint(10L, 1L, null);

        verify(userServiceClient, never()).usePoint(anyLong(), any());
    }

    @Test
    @DisplayName("포인트 롤백 - 포인트가 유효할 때만 RabbitMQ 메시지 전송")
    void rollbackPoint_Success() {
        resourceManager.rollbackPoint(10L, 1L, 500);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_POINT), any(OrderCanceledEvent.class));
    }

    @Test
    @DisplayName("쿠폰 확정 - 쿠폰 ID가 null이면 사용 API를 호출하지 않는다")
    void confirmCoupon_NullId_NoCall() {
        Long userId = 1L;
        OrderCreateRequestDto req = mock(OrderCreateRequestDto.class);
        OrderVerificationResult result = mock(OrderVerificationResult.class);

        when(req.getOrderItems()).thenReturn(List.of(new OrderItemRequestDto(100L, 1, false, null)));
        when(req.getMemberCouponId()).thenReturn(null); // 쿠폰 ID 없음
        when(result.orderNumber()).thenReturn("ORD-002");

        resourceManager.prepareResources(userId, req, result, 10L);

        verify(couponServiceClient, never()).useCoupon(any(), any(), any());
    }
}