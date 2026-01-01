package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.PaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.publisher.PaymentEventPublisher;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentCancelRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;

    private final OrderTransactionService orderTransactionService;
    private final OrderResourceManager orderResourceManager;
    private final PaymentEventPublisher paymentEventPublisher;

    private final int MAX_RETRY = 2;

    // 결제 저장 로직
    @Transactional
    public Payment savePaymentAndPublishEvent(String provider, CommonResponse commonResponse, Order order){
        log.debug("결제 내역 DB 저장 로직 실행");

        PaymentCreateRequest paymentCreateRequest = commonResponse.toPaymentCreateRequest(provider);
        Payment payment = new Payment(paymentCreateRequest);

        try {
            // 3. 결제 저장
            Payment savedPayment = paymentRepository.save(payment);

            // 4. 주문 및 주문 항목 상태 변경 (동기)
            orderTransactionService.changeStatusOrder(order, true);

            // 5-1. 외부 리소스 확정 (메시지)
            orderResourceManager.completeOrder(order.getOrderNumber());

            // 5-2. 내부 이벤트 발행 (AFTER_COMMIT)
            paymentEventPublisher.publishSuccessPayment(order);

            // 모든 작업 끝난 뒤 return
            return savedPayment;

        } catch (Exception e) {
            log.error("결제 내역 DB 저장 중 오류 : {}", e.getMessage());
            throw new PaymentException("결제 내역 DB 저장 중 오류, 관리자를 호출하세요 " + e.getMessage());
        }
    }

    // 결제 취소 저장 로직
    @Transactional
    public List<PaymentCancelResponse> savePaymentCancel(PaymentCancelCreateRequest req) {
        log.debug("결제 취소 내역 저장 로직 실행");
        List<PaymentCancel> cancelList = new ArrayList<>();

        for (Cancel cancel : req.cancels()) {
            cancelList.add(
                    new PaymentCancel(req.paymentKey(),
                            cancel.cancelAmount(),
                            cancel.cancelReason(),
                            cancel.canceledAt()
                    )
            );
        }

        int count = 0;

        while(count <= MAX_RETRY){
            try{
                List<PaymentCancel> saved = paymentCancelRepository.saveAll(cancelList);

                return saved.stream()
                        .map(PaymentCancel::toResponse)
                        .toList();
            } catch (Exception e) {
                log.error("결제 내역 DB 저장 중 오류 : {}", e.getMessage());
                count++;
            }
        }

        throw new PaymentException("결제 내역 DB 저장 재시도 횟수 초과, 관리자를 호출하세요");
    }
}
