package com.nhnacademy.book2onandon_order_payment_service.payment.publisher;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;

/**
 * 도메인 전용 퍼블리셔 인터페이스
 */
public interface PaymentEventPublisher {
    void publishSuccessPayment(Order order);
}
