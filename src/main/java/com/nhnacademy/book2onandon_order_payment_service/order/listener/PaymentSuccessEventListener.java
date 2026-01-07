package com.nhnacademy.book2onandon_order_payment_service.order.listener;

import com.nhnacademy.book2onandon_order_payment_service.cart.service.CartService;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.DeliveryService;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.event.PaymentSuccessEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentSuccessEventListener {

//    private final DeliveryService deliveryService;
    private final DeliveryService deliveryService;
    private final CartService cartService;
    private final OrderRepository orderRepository;

    @Async
//    @EventListener
//    @Transactional
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void paymentSuccessHandle(PaymentSuccessEvent event){
//        log.info("결제 성공 후 배송 생성 이벤트 처리 - (주문번호 : {})", event.getOrder().getOrderNumber());
//
//        deliveryService.createPendingDelivery(event.getOrder().getOrderId());
//    }

    public void paymentSuccessHandle(PaymentSuccessEvent event) {

        Long orderId = event.getOrder().getOrderId();
        String orderNumber = event.getOrder().getOrderNumber();

        log.info("결제 성공 이벤트 처리 시작 : orderNumber={}, orderId={}", orderNumber, orderId);

        // 배송 (기존 로직 유지)
        deliveryService.createPendingDelivery(orderId);

        // 장바구니 정리 (이게 지금까지 없었음)
//        Order order = orderRepository.findById(orderId).orElseThrow();
        Order order = event.getOrder();

        if (order.getUserId() != null) {
            List<Long> bookIds = order.getOrderItems()
                    .stream()
                    .map(OrderItem::getBookId)
                    .toList();

            cartService.deleteUserCartItemsAfterPayment(order.getUserId(), bookIds);
        }

        log.info("결제 성공 후 배송 생성 이벤트 처리 - orderNumber : {}, orderId={}", orderNumber, orderId);
    }
}
