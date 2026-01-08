package com.nhnacademy.book2onandon_order_payment_service.order.listener;

import com.nhnacademy.book2onandon_order_payment_service.cart.service.CartService;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.service.DeliveryService;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.event.PaymentSuccessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.Mockito.*;

class PaymentSuccessEventListenerTest {

    private DeliveryService deliveryService;
    private CartService cartService;
    private PaymentSuccessEventListener listener;

    @BeforeEach
    void setUp() {
        deliveryService = mock(DeliveryService.class);
        cartService = mock(CartService.class);

        listener = new PaymentSuccessEventListener(deliveryService, cartService);
    }

    @Test
    @DisplayName("결제 성공 이벤트 처리 시 배송 서비스 호출 및 장바구니 정리")
    void paymentSuccessHandle_ShouldCallDeliveryAndClearCart() {
        // given
        OrderItem item1 = OrderItem.builder().build();
        item1.setBookId(101L);

        OrderItem item2 = OrderItem.builder().build();
        item2.setBookId(102L);

        Order order = Order.builder()
                .orderId(1L)
                .orderNumber("B2-000000000001")
                .userId(1001L)
                .orderItems(List.of(item1, item2))
                .build();

        PaymentSuccessEvent event = new PaymentSuccessEvent(order);

        // when
        listener.paymentSuccessHandle(event);

        // then
        // 배송 서비스 호출 확인
        verify(deliveryService, times(1)).createPendingDelivery(order.getOrderId());

        // 장바구니 삭제 호출 확인
        ArgumentCaptor<List<Long>> bookIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cartService, times(1))
                .deleteUserCartItemsAfterPayment(eq(order.getUserId()), bookIdsCaptor.capture());

        List<Long> capturedBookIds = bookIdsCaptor.getValue();
        assert capturedBookIds.contains(101L);
        assert capturedBookIds.contains(102L);
    }

    @Test
    @DisplayName("결제 성공 이벤트 처리 시 UserId가 없는 주문이면 장바구니 삭제 호출 안 함")
    void paymentSuccessHandle_ShouldNotCallCartService_WhenUserIdIsNull() {
        // given
        Order order = Order.builder()
                .orderId(2L)
                .orderNumber("B2-000000000002")
                .userId(null) // 비회원
                .build();

        PaymentSuccessEvent event = new PaymentSuccessEvent(order);

        // when
        listener.paymentSuccessHandle(event);

        // then
        verify(deliveryService, times(1)).createPendingDelivery(order.getOrderId());
        verify(cartService, never()).deleteUserCartItemsAfterPayment(anyLong(), anyList());
    }
}
