package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.event;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentSuccessEvent {
    private final Order order;
}
