package com.nhnacademy.book2onandon_order_payment_service.order.order.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.listener.PaymentSuccessEventListener;
import com.nhnacademy.book2onandon_order_payment_service.order.service.DeliveryService;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.event.PaymentSuccessEvent;
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

    @InjectMocks
    private PaymentSuccessEventListener eventListener;

    @Test
    @DisplayName("결제 성공 이벤트 수신 시 배송 생성 서비스를 호출한다")
    void paymentSuccessHandle_Success() {
        Long orderId = 100L;
        String orderNumber = "ORD-123";
        Order mockOrder = mock(Order.class);
        PaymentSuccessEvent event = new PaymentSuccessEvent(mockOrder);

        given(mockOrder.getOrderId()).willReturn(orderId);
        given(mockOrder.getOrderNumber()).willReturn(orderNumber);

        eventListener.paymentSuccessHandle(event);

        verify(deliveryService, times(1)).createPendingDelivery(orderId);
    }

    @Test
    @DisplayName("이벤트 내 Order 객체가 비어있을 경우 예외가 발생한다 (Fail Path)")
    void paymentSuccessHandle_Fail_NullOrder() {
        PaymentSuccessEvent event = new PaymentSuccessEvent(null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> eventListener.paymentSuccessHandle(event))
                .isInstanceOf(NullPointerException.class);
    }
}