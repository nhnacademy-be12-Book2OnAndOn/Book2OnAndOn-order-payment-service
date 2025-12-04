package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.client.TossPaymentsApiClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdatePaymentStatusRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.AmountMismatchException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.property.TossPaymentsProperties;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentStrategy implements PaymentStrategy{

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final TossPaymentsApiClient tossPaymentsApiClient;
    private final TossPaymentsProperties properties;

    @Override
    public String getProvider() {
        return "TOSS";
    }

    @Override
    public CommonConfirmResponse confirmAndProcessPayment(CommonConfirmRequest req) {
        log.info("토스 결제 승인 시작\norderId : {}\npaymentKey : {}\namount : {}", req.orderId(), req.paymentKey(), req.amount());
        // 보안 헤더 생성
        String authorization = buildAuthorizationHeader();

        Integer totalAmount = orderService.getTotalAmount(req.orderId());

        // 금액 검증
        if(Objects.equals(totalAmount, req.amount())){
            log.error("결제금액과 주문금액이 같지 않습니다");
            throw new AmountMismatchException("결제 금액과 주문금액이 같지 않습니다");
        }

        // 공통 요청 -> 토스 승인 요청 변환
        TossConfirmRequest tossConfirmRequest = req.toTossConfirmRequest();

        // API 호출
        log.info("Toss Payments API 승인 요청");
        TossConfirmResponse tossConfirmResponse = tossPaymentsApiClient.confirmPayment(authorization, tossConfirmRequest);
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
        paymentService.updatePaymentStatus(updateStatusReq);

        PaymentUpdateRefundAmountRequest updateRefundAmountReq = new PaymentUpdateRefundAmountRequest(orderNumber, req.paymentKey());
        paymentService.updateRefundAmount(updateRefundAmountReq);

        // TODO 이벤트 핸들러 작성

        return cancelResponse.toCommonCancelResponse();
    }

    private String buildAuthorizationHeader(){
        String encodeSecretKey = Base64.getEncoder().encodeToString((properties.getSecretKey() + ":").getBytes(
                StandardCharsets.UTF_8));
        return "Basic " + encodeSecretKey;
    }
}
