package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.OrderNumberGenerator;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService2{

    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    @Override
    public OrderSimpleDto createPreOrder(Long userId) {
        Order order = Order.builder()
                .userId(userId)
                .orderNumber(orderNumberGenerator.generate())
                .orderStatus(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);
        return saved.toOrderSimpleDto();
    }
}
