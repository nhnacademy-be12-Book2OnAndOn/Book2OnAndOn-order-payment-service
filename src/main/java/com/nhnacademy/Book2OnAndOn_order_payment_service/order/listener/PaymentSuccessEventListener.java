package com.nhnacademy.Book2OnAndOn_order_payment_service.order.listener;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.DeliveryService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentSuccessEventListener {

    private final DeliveryService deliveryService;

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void paymentSuccessHandle(PaymentSuccessEvent event){
        log.info("결제 성공 후 배송 생성 이벤트 처리 - (주문번호 : {})", event.getOrder().getOrderNumber());

        deliveryService.createPendingDelivery(event.getOrder().getOrderId());
    }
}
