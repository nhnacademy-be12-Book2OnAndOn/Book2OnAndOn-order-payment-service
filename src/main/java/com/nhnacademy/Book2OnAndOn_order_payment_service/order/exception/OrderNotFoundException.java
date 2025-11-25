package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

// 특정 주문 ID로 주문을 찾을 수 없을 때
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException() {
        super("주문을 찾을 수 없습니다.");
    }
    public OrderNotFoundException(Long orderId) {
        super("주문을 찾을 수 없습니다. (ID: " + orderId + ")");
    }
}