package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdatePaymentStatusRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.DuplicatePaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    // 주문조회시 결제정보도 출력
    @Override
    public PaymentResponse getPayment(PaymentRequest req) {
        log.info("결제 단건 정보 가져오기 (주문번호 : {})", req.orderNumber());
        Payment payment = validateAndGetPayment(req.orderNumber(), true);

        log.info("결제 단건 정보 (주문번호 : {}, 상태 : {})", payment.getOrderNumber(), payment.getPaymentStatus().name());
        return payment.toResponse();
    }

    // 결제 성공 혹은 실패시 생성할 결제 정보
    @Override
    public PaymentResponse createPayment(PaymentCreateRequest req) {
        log.info("결제 정보 생성 (주문번호 : {}, 결제금액 : {})", req.orderNumber(), req.totalAmount());
        validateAndGetPayment(req.orderNumber(), false);
        Payment payment = new Payment(req);
        log.info("결제 엔티티 생성 : {}", payment);
        Payment saved = paymentRepository.save(payment);
        log.info("결제 정보 저장 (주문번호 : {}, 결제상태 : {})", saved.getOrderNumber(), saved.getPaymentStatus());

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
    @Transactional
    @Override
    public PaymentResponse updateRefundAmountPayment(PaymentUpdateRefundAmountRequest req, Integer refundAmount) {
        log.info("결제 환불 금액 업데이트 (주문번호 : {})", req.orderNumber());
        Payment payment = validateAndGetPayment(req.orderNumber(), true);
        payment.setRefundAmount(refundAmount);
        log.info("결제 환불 금액 업데이트 성공 (주문번호 : {}, 환불금액 : {})", payment.getOrderNumber(), payment.getRefundAmount());
        return payment.toResponse();
    }

    // 같은 결제 건의 상태 변경이 필요함
    @Transactional
    @Override
    public PaymentResponse updatePaymentStatus(PaymentUpdatePaymentStatusRequest req) {
        log.info("결제 상태 업데이트 (주문번호 : {})", req.orderNumber());
        Payment payment = validateAndGetPayment(req.orderNumber(), true);
        payment.setPaymentStatus(PaymentStatus.fromExternal(req.PaymentStatus()));
        log.info("결제 상태 업데이트 성공 (주문번호 : {}, 결제상태 : {})", payment.getOrderNumber(), payment.getRefundAmount());

        return payment.toResponse();
    }



    private Payment validateAndGetPayment(String orderNumber, boolean shouldExist){
        Payment payment = paymentRepository.findByOrderNumber(orderNumber);

        if(shouldExist && Objects.isNull(payment)){
            // 조회 및 삭제 : 데이터가 없으면 안됨
            log.warn("결제 정보를 찾을 수 없습니다 (주문번호 : {})", orderNumber);
            throw new NotFoundPaymentException("Not Found Payment : " + orderNumber);
        }

        if(!shouldExist && Objects.nonNull(payment)){
            // 생성 : 중복 데이터가 있으면 안됨
            log.warn("중복 결제 데이터입니다 (주문번호 : {})", orderNumber);
            throw new DuplicatePaymentException("Duplicate Payment Data : " + orderNumber);
        }

        return payment;
    }
}