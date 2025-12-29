package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.PaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
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

    private final int MAX_RETRY = 2;

    // 결제 저장 로직
    @Transactional
    public Payment savePayment(String provider, CommonResponse commonResponse){
        log.debug("결제 내역 DB 저장 로직 실행");

        PaymentCreateRequest paymentCreateRequest = commonResponse.toPaymentCreateRequest(provider);
        Payment payment = new Payment(paymentCreateRequest);

        int count = 0;

        while(count <= MAX_RETRY){
            try{
                return paymentRepository.save(payment);
            } catch (Exception e) {
                log.error("결제 내역 DB 저장 중 오류 : {}", e.getMessage());
                count++;
            }
        }

        throw new PaymentException("결제 내역 DB 저장 재시도 횟수 초과, 관리자를 호출하세요");
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
