package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.EarnOrderPointRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.PaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.DuplicatePaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.publisher.PaymentEventPublisher;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStrategyFactory factory;
    private final PaymentTransactionService paymentTransactionService;
    private final OrderTransactionService orderTransactionService;

    private final OrderResourceManager orderResourceManager;
    private final UserServiceClient userServiceClient;

    private static final int MAX_TRY = 5;

    // 주문조회시 결제정보도 출력
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(PaymentRequest req) {
        log.info("결제 단건 정보 가져오기 (주문번호 : {})", req.orderNumber());
        Payment payment = validateAndGetPayment(req.orderNumber(), true);

        log.info("결제 단건 정보 (주문번호 : {}, 상태 : {})", payment.getOrderNumber(), payment.getPaymentStatus().name());
        return payment.toResponse();
    }

//    @Override
//    @Transactional
//    public PaymentResponse confirmAndCreatePayment(String provider, CommonConfirmRequest req) {
//        log.info("결제 승인 요청 및 결제 엔티티 생성 (주문번호 : {})", req.orderId());
//        // 1. 주문 금액 검증
//        Order order = orderTransactionService.validateOrderAmount(req);
//        try {
//            // 2. 결제 승인 요청 (5회 재시도 후 오류시 관리자 호출)
//            CommonResponse commonResponse = confirmPaymentWithRetry(provider, req);
//
//            // 여기서만 포인트 확정 차감
////            orderResourceManager.confirmPoint(order.getOrderId(), order.getUserId(), order.getPointDiscount());
//
//            // 3. DB 저장 요청 (2회 재시도 후 오류시 관리자 호출)
//            Payment saved = paymentTransactionService.savePaymentAndPublishEvent(provider, commonResponse, order);
//
//            if (order.getUserId() != null) {
//                int pureAmount = resolvePureAmountForEarn(order);
//
//                userServiceClient.earnOrderPoint(
//                        order.getUserId(),
//                        new EarnOrderPointRequestDto(null, order.getOrderId(), pureAmount)
//                );
//            }
//
//            return saved.toResponse();
//        } catch(Exception e) {
//            //  결제 승인/저장 실패 시 보상
//            orderResourceManager.rollbackPoint(order.getOrderId(), order.getUserId(), order.getPointDiscount());
//
//            // 필요시 상태도 FAIL/CANCELED로 전환(정책에 맞게)
//            orderTransactionService.changeStatusOrder(order, false);
//
//            throw e;
//        }
//    }

    @Override
    @Transactional
    public PaymentResponse confirmAndCreatePayment(String provider, CommonConfirmRequest req) {
        log.info("결제 승인 요청 및 결제 엔티티 생성 (주문번호 : {})", req.orderId());
        // 1. 주문 금액 검증
        Order order = orderTransactionService.getOrderEntity(req.orderId());

        try {
            order = orderTransactionService.validateOrderAmount(req);
        } catch(Exception e) {
            //  결제 승인/저장 실패 시 보상
            orderResourceManager.releaseResources(
                    order.getOrderNumber(),
                    order.getUserId(),
                    order.getPointDiscount(),
                    order.getOrderId()
            );

            // 필요시 상태도 FAIL/CANCELED로 전환(정책에 맞게)
            orderTransactionService.changeStatusOrder(order, false);

            throw e;
        }

        // 2. 결제 승인 요청 (5회 재시도 후 오류시 관리자 호출)
        CommonResponse commonResponse = confirmPaymentWithRetry(provider, req);

        // 3. DB 저장 요청 (2회 재시도 후 오류시 관리자 호출)
        Payment saved = paymentTransactionService.savePaymentAndPublishEvent(provider, commonResponse, order);

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

    @Override
    @Transactional
    public void cancelPayment(PaymentCancelRequest req) {
        log.info("주문 취소 로직 실행 (주문번호 : {})", req.orderNumber());

        // 취소할 결제 호출
        Payment payment = paymentRepository.findByOrderNumber(req.orderNumber())
                .orElseThrow(() -> new NotFoundPaymentException("Not Found Payment : " + req.orderNumber()));

        String provider = payment.getPaymentProvider().name();

        CommonCancelRequest commonCancelRequest = new CommonCancelRequest(payment.getPaymentKey(), req.amount(), req.reason());

        // 결제 취소 승인 요청 (5회 재시도 후 오류시 관리자 호출)
        CommonCancelResponse commonCancelResponse = cancelPaymentWithRetry(provider, commonCancelRequest);

        PaymentCancelCreateRequest paymentCancelCreateRequest = commonCancelResponse.toPaymentCancelCreateRequest();

        // 결제 취소 DB 저장 (2회 재시도 후 오류시 관리자 호출)
        List<PaymentCancelResponse> paymentCancelResponseList = paymentTransactionService.savePaymentCancel(paymentCancelCreateRequest);

        // 결제 취소 금액 업데이트
        int refundAmount = paymentCancelResponseList.stream()
                .mapToInt(PaymentCancelResponse::cancelAmount)
                .sum();

        payment.setRefundAmount(refundAmount);
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

    private CommonCancelResponse cancelPaymentWithRetry(String provider, CommonCancelRequest req){
        log.debug("결제 취소 승인 요청 로직 실행");
        String idempotencyKey = UUID.randomUUID().toString();
        log.debug("멱등키 : {}", idempotencyKey);

        int retryCount = 0;
        PaymentStrategy paymentStrategy = factory.getStrategy(provider);

        while(retryCount < MAX_TRY){
            try{
                // 승인된 결제 상태만 반환
                return paymentStrategy.cancelPayment(req, idempotencyKey);
            } catch (Exception e) {
                retryCount++;
                log.error("결제 승인 처리 중 오류 발생 (재시도 횟수 : {}/{}), {}", retryCount, MAX_TRY, e.getMessage());
            }
        }
        throw new PaymentException("결제 승인 최대 재시도 횟수 초과, 관리자를 호출해주세요");
    }
}