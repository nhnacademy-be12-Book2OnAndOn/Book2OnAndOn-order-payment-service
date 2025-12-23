package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.PaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.RabbitConfig;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdatePaymentStatusRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.DuplicatePaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentCancelRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentSuccessEventHandler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentStrategyFactory factory;

    private final OrderRepository orderRepository;
    private final OrderResourceManager orderResourceManager;
    private final PaymentSuccessEventHandler successEventHandler;
    private final RabbitTemplate rabbitTemplate;
    private static final int MAX_TRY = 5;

    // 주문조회시 결제정보도 출력
    @Override
    public PaymentResponse getPayment(PaymentRequest req) {
        log.info("결제 단건 정보 가져오기 (주문번호 : {})", req.orderNumber());
        Payment payment = validateAndGetPayment(req.orderNumber(), true);

        log.info("결제 단건 정보 (주문번호 : {}, 상태 : {})", payment.getOrderNumber(), payment.getPaymentStatus().name());
        return payment.toResponse();
    }

    @Override
    public PaymentResponse confirmAndCreatePayment(String provider, CommonConfirmRequest req) {
        log.info("결제 승인 요청 및 결제 엔티티 생성 (주문번호 : {})", req.orderId());
        // TODO 결제 성공후 이벤트 구현하기
        // 1. 주문 금액 검증
        paymentTransactionService.validateOrderAmount(req);

        // 2. 결제 승인 요청 (5회 재시도 후 오류시 관리자 호출)
        CommonResponse commonResponse = confirmPaymentWithRetry(provider, req);

        // 3. DB 저장 요청 (2회 재시도 후 오류시 관리자 호출)
        Payment saved = savePayment(provider, commonResponse);

        // 4. 이벤트 핸들러 구현
        // 외부
        orderResourceManager.finalizeBooks(req.orderId());
        successEventHandler.successHandler(req.orderId());

        return saved.toResponse();
    }

    // 결제 내역 삭제
    @Override
    public PaymentDeleteResponse deletePayment(PaymentDeleteRequest req) {
        log.info("결제 삭제 시도 (주문번호 : {})", req.orderNumber());
        Payment payment = validateAndGetPayment(req.orderNumber(), true);
        paymentRepository.delete(payment);
        log.info("결제 내역 삭제");

        return new PaymentDeleteResponse(payment.getOrderNumber());
    }

    // 환불 금액 업데이트
//    @Transactional
//    @Override
//    public PaymentResponse updateRefundAmount(PaymentUpdateRefundAmountRequest req) {
//        log.info("결제 환불 금액 업데이트 (주문번호 : {})", req.orderNumber());
//        Payment payment = validateAndGetPayment(req.orderNumber(), true);
//
//        List<PaymentCancel> cancelList = paymentCancelRepository.findByPaymentKey(req.paymentKey());
//        log.info("결제 취소 사이즈 : {}", cancelList.size());
//
//        Integer refundAmount = cancelList.stream()
//                .mapToInt(PaymentCancel::getCancelAmount)
//                .sum();
//
//        payment.setRefundAmount(refundAmount);
//        log.info("결제 환불 금액 업데이트 성공 (주문번호 : {}, 환불금액 : {})", payment.getOrderNumber(), payment.getRefundAmount());
//        return payment.toResponse();
//    }
//
//    // 같은 결제 건의 상태 변경이 필요함
//    @Transactional
//    @Override
//    public PaymentResponse updatePaymentStatus(PaymentUpdatePaymentStatusRequest req) {
//        log.info("결제 상태 업데이트 (주문번호 : {})", req.orderNumber());
//        Payment payment = validateAndGetPayment(req.orderNumber(), true);
//        payment.setPaymentStatus(PaymentStatus.fromExternal(req.PaymentStatus()));
//        log.info("결제 상태 업데이트 성공 (주문번호 : {}, 결제상태 : {})", payment.getOrderNumber(), payment.getRefundAmount());
//
//        return payment.toResponse();
//    }

    // 결제 취소 로직
    @Override
    public List<PaymentCancelResponse> createPaymentCancel(PaymentCancelCreateRequest req) {
        log.info("결제 취소 생성 시작 (결제키 : {})", req.paymentKey());
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
        List<PaymentCancel> saved = paymentCancelRepository.saveAll(cancelList);
        log.info("결제 취소 생성 완료 (크기 : {})", saved.size());
        return saved.stream()
                .map(PaymentCancel::toResponse)
                .collect(Collectors.toList());
    }

    // 결제 취소 내역 확인
    @Override
    public List<PaymentCancelResponse> getCancelPaymentList(String paymentKey){
        log.info("결제 취소 내역 조회 시작 (결제키 : {})", paymentKey);
        List<PaymentCancel> cancelList = paymentCancelRepository.findByPaymentKey(paymentKey);

        return cancelList.stream()
                .map(PaymentCancel::toResponse)
                .collect(Collectors.toList());
    }


    @Override
    public String getProvider(String orderNumber) {
        return paymentRepository.findProviderByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundPaymentException("Not Found Payment : " + orderNumber));
    }

    // ============== 헬퍼 메서드 ==============
    private Payment validateAndGetPayment(String orderNumber, boolean shouldExist){
        Optional<Payment> optionalPayment = paymentRepository.findByOrderNumber(orderNumber);

        if(shouldExist && optionalPayment.isEmpty()){
            // 조회 및 삭제 : 데이터가 없으면 안됨
            log.warn("결제 정보를 찾을 수 없습니다 (주문번호 : {})", orderNumber);
            throw new NotFoundPaymentException("Not Found Payment : " + orderNumber);
        }

        if(!shouldExist && optionalPayment.isPresent()){
            // 생성 : 중복 데이터가 있으면 안됨
            log.warn("중복 결제 데이터입니다 (주문번호 : {})", orderNumber);
            throw new DuplicatePaymentException("Duplicate Payment Data : " + orderNumber);
        }

        return optionalPayment.orElse(null);
    }

    // 승인 요청
    private CommonResponse confirmPaymentWithRetry(String provider, CommonConfirmRequest req){
        log.debug("결제 승인 요청 로직 실행");
        String idempotencyKey = UUID.randomUUID().toString();
        log.debug("멱등키 : {}", idempotencyKey);

        int retryCount = 0;
        PaymentStrategy paymentStrategy = factory.getStrategy(provider);

        while(retryCount < MAX_TRY){
            try{
                // 승인된 결제 상태만 반환
                return paymentStrategy.confirmPayment(req, idempotencyKey);
            } catch (Exception e) {
                retryCount++;
                log.error("결제 승인 처리 중 오류 발생 (재시도 횟수 : {}/{}), {}", retryCount, MAX_TRY, e.getMessage());
            }
        }
        throw new PaymentException("결제 승인 최대 재시도 횟수 초과, 관리자를 호출해주세요");
    }

    // DB 저장
    @Retryable(
            value = Exception.class,
            maxAttempts = 2,
            backoff = @Backoff(delay = 300)
    )
    private Payment savePayment(String provider, CommonResponse commonResponse){
        log.debug("결제 내역 DB 저장 로직 실행");
        try {
            PaymentCreateRequest paymentCreateRequest = commonResponse.toPaymentCreateRequest(provider);
            Payment payment = new Payment(paymentCreateRequest);
            return paymentRepository.save(payment);
        }catch (Exception e){
            log.error("결제 내역 DB 저장 중 오류 발생! 관리자를 호출하세요");
            throw new PaymentException("결제 내역 DB 저장 재시도 횟수 초과, 관리자를 호출해주세요" + e.getMessage());
        }
    }
}