package com.nhnacademy.book2onandon_order_payment_service.payment.publisher;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring Event 기반 구현체
 */
@Component
@RequiredArgsConstructor
public class SpringPaymentEventPublisher implements PaymentEventPublisher{

    private final ApplicationEventPublisher publisher;

    @Override
    public void publishSuccessPayment(Order order) {
        publisher.publishEvent(new PaymentSuccessEvent(order));
    }
}
