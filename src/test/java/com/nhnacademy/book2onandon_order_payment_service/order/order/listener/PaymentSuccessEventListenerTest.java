package com.nhnacademy.book2onandon_order_payment_service.order.order.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.cart.service.CartService;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.listener.PaymentSuccessEventListener;
import com.nhnacademy.book2onandon_order_payment_service.order.service.DeliveryService;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.event.PaymentSuccessEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentSuccessEventListenerTest {

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private CartService cartService;

    @InjectMocks
    private PaymentSuccessEventListener eventListener;

    @Test
    @DisplayName("결제 성공 이벤트 수신 시 배송 생성 및 장바구니 정리를 수행한다")
    void paymentSuccessHandle_Success() {
        Long orderId = 100L;
        Long userId = 1L;
        String orderNumber = "ORD-123";

        Order mockOrder = mock(Order.class);
        OrderItem mockItem = mock(OrderItem.class);
        PaymentSuccessEvent event = new PaymentSuccessEvent(mockOrder);

        given(mockOrder.getOrderId()).willReturn(orderId);
        given(mockOrder.getOrderNumber()).willReturn(orderNumber);
        given(mockOrder.getUserId()).willReturn(userId);
        given(mockOrder.getOrderItems()).willReturn(List.of(mockItem));
        given(mockItem.getBookId()).willReturn(50L);

        eventListener.paymentSuccessHandle(event);

        verify(deliveryService, times(1)).createPendingDelivery(orderId);
        verify(cartService, times(1)).deleteUserCartItemsAfterPayment(eq(userId), anyList());
    }

    @Test
    @DisplayName("비회원 주문인 경우 장바구니 정리를 호출하지 않는다")
    void paymentSuccessHandle_GuestOrder_NoCartClear() {
        Order mockOrder = mock(Order.class);
        PaymentSuccessEvent event = new PaymentSuccessEvent(mockOrder);

        given(mockOrder.getOrderId()).willReturn(100L);
        given(mockOrder.getOrderNumber()).willReturn("ORD-GUEST");
        given(mockOrder.getUserId()).willReturn(null);

        eventListener.paymentSuccessHandle(event);

        verify(deliveryService, times(1)).createPendingDelivery(100L);
        verify(cartService, times(0)).deleteUserCartItemsAfterPayment(any(), any());
    }

    @Test
    @DisplayName("이벤트 내 Order 객체가 비어있을 경우 예외가 발생한다")
    void paymentSuccessHandle_Fail_NullOrder() {
        PaymentSuccessEvent event = new PaymentSuccessEvent(null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> eventListener.paymentSuccessHandle(event))
                .isInstanceOf(NullPointerException.class);
    }
}