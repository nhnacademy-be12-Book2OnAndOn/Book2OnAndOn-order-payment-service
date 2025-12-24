package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.PaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryAddressRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSuccessEventHandler {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    @Async
    public void successHandler(String orderNumber){
        try{
            Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException("해당 주문을 찾을 수 없습니다 : " + orderNumber));
            // 주문 상태 변경 및 주문 항목 상태 변경
            changeStatus(order);
            // 배송 엔티티 생성
            createDelivery(order);
        } catch (Exception e) {
            log.error("결제 성공 이벤트 핸들러 오류 : {}", e.getMessage());
            throw new PaymentException("결제 성공 이벤트 핸들러 오류, " + e.getMessage());
        }

    }

    private void changeStatus(Order order){
        changeOrderStatus(order);
        changeOrderItemStatus(order.getOrderItems());
    }

    private void createDelivery(Order order){
        deliveryRepository.save(new Delivery(order));
    }

    private void changeOrderStatus(Order order){
        order.setOrderStatus(OrderStatus.COMPLETED);
    }

    private void changeOrderItemStatus(List<OrderItem> orderItemList){
        for (OrderItem orderItem : orderItemList) {
            orderItem.setOrderItemStatus(OrderItemStatus.ORDER_COMPLETE);
        }
    }


}
