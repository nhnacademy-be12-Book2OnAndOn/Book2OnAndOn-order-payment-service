package com.nhnacademy.book2onandon_order_payment_service.payment.publisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.event.PaymentSuccessEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringPaymentEventPublisherTest {

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private SpringPaymentEventPublisher springPaymentEventPublisher;

    @Test
    @DisplayName("결제 성공 시 주문 정보를 포함한 PaymentSuccessEvent가 발행되어야 한다")
    void publishSuccessPayment_Success() {
        Order order = mock(Order.class);

        springPaymentEventPublisher.publishSuccessPayment(order);

        verify(publisher).publishEvent(any(PaymentSuccessEvent.class));
    }
}