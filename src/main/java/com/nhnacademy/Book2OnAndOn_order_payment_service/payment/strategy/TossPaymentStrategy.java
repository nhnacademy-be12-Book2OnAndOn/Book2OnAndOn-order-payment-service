package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy;

import static com.netflix.spectator.api.Statistic.totalAmount;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.client.TossPaymentsApiClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdatePaymentStatusRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.AmountMismatchException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.property.TossPaymentsProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentStrategy implements PaymentStrategy{

    private final TossPaymentsApiClient tossPaymentsApiClient;
    private final TossPaymentsProperties properties;

    @Override
    public String getProvider() {
        return "TOSS";
    }

    @Override
    public CommonResponse confirmPayment(CommonConfirmRequest req, String idempontencyKey) {
        log.info("토스 결제 승인 시작\norderId : {}\npaymentKey : {}\namount : {}", req.orderId(), req.paymentKey(), req.amount());
        // 보안 헤더 생성
        String authorization = buildAuthorizationHeader();

        // 공통 요청 -> 토스 승인 요청 변환
        TossConfirmRequest tossConfirmRequest = req.toTossConfirmRequest();

        // API 호출
        log.info("Toss Payments API 승인 요청");
        TossResponse tossConfirmResponse = tossPaymentsApiClient.confirmPayment(authorization, idempontencyKey, tossConfirmRequest);
        log.info("Toss Payments API 승인 성공");

        // 결제사 응답값 -> 공용 db 처리
        return tossConfirmResponse.toCommonConfirmResponse();
    }

    @Override
    public CommonCancelResponse cancelPayment(CommonCancelRequest req, String orderNumber) {
        log.info("토스 결제 취소 시작 : (paymentKey : {}, orderNumber : {}, cancelReason : {}, cancelAmount : {})", req.paymentKey(), orderNumber, req.cancelReason(), req.cancelAmount());

        String authorization = buildAuthorizationHeader();
        TossCancelRequest cancelRequest = req.toTossCancelRequest();
        TossCancelResponse cancelResponse = tossPaymentsApiClient.cancelPayment(authorization, req.paymentKey(), cancelRequest);

        // 결제 취소시 결제 상태 변경
        PaymentUpdatePaymentStatusRequest updateStatusReq = new PaymentUpdatePaymentStatusRequest(orderNumber, cancelResponse.status());
//        paymentService.updatePaymentStatus(updateStatusReq);

        PaymentUpdateRefundAmountRequest updateRefundAmountReq = new PaymentUpdateRefundAmountRequest(orderNumber, req.paymentKey());
//        paymentService.updateRefundAmount(updateRefundAmountReq);

        // TODO 이벤트 핸들러 작성

        return cancelResponse.toCommonCancelResponse();
    }

    // 인증 헤더 생성
    private String buildAuthorizationHeader(){
        String encodeSecretKey = Base64.getEncoder().encodeToString((properties.getSecretKey() + ":").getBytes(
                StandardCharsets.UTF_8));
        return "Basic " + encodeSecretKey;
    }
}
