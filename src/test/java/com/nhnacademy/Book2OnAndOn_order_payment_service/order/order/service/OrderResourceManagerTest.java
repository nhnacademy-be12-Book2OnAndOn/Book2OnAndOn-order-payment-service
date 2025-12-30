package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.ReserveBookRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UseCouponRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UsePointInternalRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.config.RabbitConfig;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.OrderCanceledEvent;
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

    @Mock private BookServiceClient bookServiceClient;
    @Mock private CouponServiceClient couponServiceClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private OrderResourceManager orderResourceManager;

    @Test
    @DisplayName("자원 준비 시 도서 예약, 쿠폰 사용, 포인트 사용 서비스를 모두 호출한다")
    void prepareResources_Success() {
        Long userId = 1L;
        Long orderId = 10L;
        Long couponId = 100L;
        OrderCreateRequestDto req = mock(OrderCreateRequestDto.class);
        OrderVerificationResult result = mock(OrderVerificationResult.class);
        OrderItemRequestDto itemReq = mock(OrderItemRequestDto.class);

        given(req.getOrderItems()).willReturn(List.of(itemReq));
        given(req.getMemberCouponId()).willReturn(couponId);
        given(result.orderNumber()).willReturn("ORD-001");
        given(result.pointDiscount()).willReturn(500);

        orderResourceManager.prepareResources(userId, req, result, orderId);

        verify(bookServiceClient).reserveStock(any(ReserveBookRequestDto.class));
        verify(couponServiceClient).useCoupon(eq(couponId), eq(userId), any(UseCouponRequestDto.class));
        verify(userServiceClient).usePoint(eq(userId), any(UsePointInternalRequestDto.class));
    }

    @Test
    @DisplayName("자원 준비 시 쿠폰이나 포인트가 없으면 해당 서비스 호출을 건너뛴다")
    void prepareResources_SkipOptional() {
        OrderCreateRequestDto req = mock(OrderCreateRequestDto.class);
        OrderVerificationResult result = mock(OrderVerificationResult.class);
        
        given(req.getOrderItems()).willReturn(List.of());
        given(req.getMemberCouponId()).willReturn(null);
        given(result.pointDiscount()).willReturn(0);

        orderResourceManager.prepareResources(null, req, result, 1L);

        verify(bookServiceClient).reserveStock(any());
        verifyNoInteractions(couponServiceClient, userServiceClient);
    }

    @Test
    @DisplayName("자원 해제 시 메시지 큐를 통해 도서, 쿠폰, 포인트 복구 이벤트를 발행한다")
    void releaseResources_Success() {
        orderResourceManager.releaseResources("ORD-001", 100L, 1L, 500, 10L);

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_BOOK), eq("ORD-001"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_COUPON), eq("ORD-001"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_CANCEL_POINT), any(OrderCanceledEvent.class));
    }

    @Test
    @DisplayName("자원 해제 시 쿠폰 ID나 포인트가 없으면 메시지 발행을 생략한다")
    void releaseResources_SkipOptional() {
        orderResourceManager.releaseResources("ORD-001", null, 1L, 0, 10L);

        verify(rabbitTemplate, times(1)).convertAndSend(any(String.class), any(String.class), any(String.class));
        verify(rabbitTemplate).convertAndSend(any(), eq(RabbitConfig.ROUTING_KEY_CANCEL_BOOK), anyString());
    }

    @Test
    @DisplayName("도서 확정 시 재고 확정 메시지를 발행한다")
    void finalizeBooks_Success() {
        orderResourceManager.finalizeBooks("ORD-001");

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY_CONFIRM_BOOK),
                eq("ORD-001")
        );
    }
}