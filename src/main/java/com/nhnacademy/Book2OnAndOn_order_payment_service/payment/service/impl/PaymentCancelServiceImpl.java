package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelListRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.DuplicatePaymentCancelException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentCancelException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentCancelRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentCancelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCancelServiceImpl implements PaymentCancelService {

    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;

    @Override
    public List<PaymentCancelResponse> getPaymentCancelList(PaymentCancelListRequest req) {
        log.info("결제 취소 리스트 가져오기 (결제번호 : {})", req.paymentId());

        Payment payment = paymentRepository.findById(req.paymentId())
                .orElseThrow(() -> {
                    log.warn("결제 정보를 찾을 수 없습니다 (결제 아이디 : {})", req.paymentId());
                    throw new NotFoundPaymentException("Not Found Payment : " + req.paymentId());
                });

        List<PaymentCancel> paymentCancelList = paymentCancelRepository.findByPaymentIdWithPayment(req.paymentId());

        if(paymentCancelList.isEmpty()){
            log.warn("결제 취소 정보가 없습니다 (결제 아이디 : {})", req.paymentId());
            throw new NotFoundPaymentCancelException("Not Found Payment Cancel List : " + req.paymentId());
        }

        log.info("결제 취소 리스트 가져오기 (결제번호 : {}, 결제 취소 개수 : {}",  req.paymentId(), paymentCancelList.size());
        return paymentCancelList
                .stream()
                .map(PaymentCancel::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentCancelResponse getPaymentCancel(PaymentCancelRequest req) {
        log.info("결제 취소 단건 정보 가져오기 (결제 아이디 : {})", req.paymentId());

        Payment payment = paymentRepository.findById(req.paymentId())
                .orElseThrow(() -> {
                    log.warn("결제 정보를 찾을 수 없습니다 (결제 아이디 : {})", req.paymentId());
                    throw new NotFoundPaymentException("Not Found Payment : " + req.paymentId());
                });

        PaymentCancel paymentCancel = paymentCancelRepository
                .findByPayment_PaymentIdAndPaymentTransactionKey(
                    req.paymentId(),
                    req.paymentTransactionKey());

        if(Objects.isNull(paymentCancel)){
            log.warn("결제 취소 정보를 찾을 수 없습니다 (결제 아이디 : {}, 결제 취소 트랜잭션 키 : {})", req.paymentId(), req.paymentTransactionKey());
            throw new NotFoundPaymentCancelException("Not Found Payment Cancel : " + req.paymentId());
        }

        log.info("결제 취소 단건 정보 (결제 아이디 : {}, 결제 취소 트랜잭션 키 : {})", paymentCancel.getPayment().getPaymentId(), paymentCancel.getPaymentTransactionKey());
        return paymentCancel.toResponse();
    }

    @Override
    public PaymentCancelResponse createPaymentCancel(PaymentCancelCreateRequest req) {

        log.info("결제 취소 정보 생성 (결제 아이디 : {}, 결제 취소 트랜잭션 키 : {})",  req.paymentId(), req.paymentTransactionKey());

        if(Objects.nonNull(paymentCancelRepository.findByPayment_PaymentIdAndPaymentTransactionKey(req.paymentId(), req.paymentTransactionKey()))){
            log.warn("중복 결제 취소 데이터입니다 (결제 아이디 : {}, 결제 취소 트랜잭션 키 : {})",  req.paymentId(), req.paymentTransactionKey());
            throw new DuplicatePaymentCancelException("Duplicate Payment Cancel Data : " + req.paymentTransactionKey());
        }


        PaymentCancel paymentCancel = new PaymentCancel(req);
        log.info("결제 취소 엔티티 생성 : {}", paymentCancel);

        PaymentCancel saved = paymentCancelRepository.save(paymentCancel);
        log.info("결제 취소 정보 저장 (결제 아이디 : {}, 결제 취소 트랜잭션 키 : {})",  saved.getPayment().getPaymentId(), saved.getPaymentTransactionKey());

        return saved.toResponse();
    }
}

// 할것 Payment Controller 만들기
// Payment Cancel Service 단위 테스트 만들기
// Payment Controller 흐름 테스트 만들기