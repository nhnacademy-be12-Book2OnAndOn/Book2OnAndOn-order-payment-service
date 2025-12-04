package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;

public interface OrderService2 {
    OrderSimpleDto createPreOrder(Long userId);

}
