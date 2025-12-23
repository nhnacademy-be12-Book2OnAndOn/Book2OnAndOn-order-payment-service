package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTransactionService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public void validateOrderAmount(CommonConfirmRequest req){
        log.debug("주문 금액 검증 실행");
        Order order = orderRepository.findByOrderNumber(req.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order Not Found : " + req.orderId()));

        if(!Objects.equals(req.amount(), order.getTotalAmount())){
            log.error("데이터베이스에 저장된 금액과 결제 금액이 일치하지 않습니다 (저장 금액 : {}, 결제 금액 : {})", order.getTotalAmount(), req.amount());
            throw new OrderVerificationException("금액 불일치, 저장 금액 : " + order.getTotalAmount() + ", 결제 금액 : " + req.amount());
        }
        log.debug("주문 금액 일치 (금액 : {})", order.getTotalAmount());
    }
}
