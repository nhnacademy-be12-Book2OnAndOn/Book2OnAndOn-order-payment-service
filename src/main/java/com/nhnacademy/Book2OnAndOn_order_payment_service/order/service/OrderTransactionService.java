package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTransactionService {

    private final OrderRepository orderRepository;
    private final OrderViewAssembler orderViewAssembler;

    // 결제 취소에 필요한 로직
    @Transactional
    public Order validateOrderExistence(Long userId, String orderNumber){
        return orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Not Found Order : " + orderNumber));
    }

    // payment건
    @Transactional
    public Order validateOrderAmount(CommonConfirmRequest req){
        log.debug("주문 금액 검증 실행");
        Order order = orderRepository.findByOrderNumber(req.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Not Found Order: " + req.orderId()));

        if(!Objects.equals(req.amount(), order.getTotalAmount())){
            log.error("데이터베이스에 저장된 금액과 결제 금액이 일치하지 않습니다 (저장 금액 : {}, 결제 금액 : {})", order.getTotalAmount(), req.amount());
            throw new OrderVerificationException("금액 불일치, 저장 금액 : " + order.getTotalAmount() + ", 결제 금액 : " + req.amount());
        }
        log.debug("주문 금액 일치 (금액 : {})", order.getTotalAmount());

        return order;
    }

    @Transactional
    public OrderCreateResponseDto createPendingOrder(Long userId, OrderVerificationResult result) {
        log.info("주문 임시 데이터 저장 로직 실행");

        Order order = Order.builder()
                .userId(userId)
                .orderNumber(result.orderNumber())
                .orderStatus(OrderStatus.PENDING)
                .orderTitle(result.orderTitle())
                .totalAmount(result.totalAmount())
                .totalDiscountAmount(result.totalDiscountAmount())
                .totalItemAmount(result.totalItemAmount())
                .deliveryFee(result.deliveryFee())
                .wrappingFee(result.wrappingFee())
                .couponDiscount(result.couponDiscount())
                .pointDiscount(result.pointDiscount())
                .wantDeliveryDate(result.wantDeliveryDate())
                .build();

        order.addOrderItem(result.orderItemList());
        order.addDeliveryAddress(result.deliveryAddress());

        Order saved = orderRepository.save(order);

        return orderViewAssembler.toOrderCreateView(saved);
    }

    @Transactional
    public void changeStatusOrder(Order order, boolean flag){
        // 상황에 따라서 flag를 INTEGER ENUM + switch 코드로 변환시킬 수 있음
        // flag == true 결제 성공
        // flag == false 결제 취소
        if(flag){
            log.info("결제 성공 후 주문 상태 변경 로직 실행");
            order.updateStatus(OrderStatus.COMPLETED);

            for (OrderItem orderItem : order.getOrderItems()) {
                orderItem.updateStatus(OrderItemStatus.ORDER_COMPLETE);
            }
        }else{
            log.info("결제 취소 후 주문 상태 변경 로직 실행");
            order.updateStatus(OrderStatus.CANCELED);

            for (OrderItem orderItem : order.getOrderItems()) {
                orderItem.updateStatus(OrderItemStatus.ORDER_CANCELED);
            }
        }

    }
}