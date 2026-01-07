package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.event;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentSuccessEvent {
    private final Order order;
}
