package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.client.TossPaymentsApiClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.ConfirmSuccessResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.property.TossPaymentsProperties;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentStrategy implements PaymentStrategy{

    private final PaymentService paymentService;
    private final TossPaymentsApiClient tossPaymentsApiClient;
    private final TossPaymentsProperties properties;

    @Override
    public String getProvider() {
        return "TOSS";
    }

    @Override
    public ConfirmSuccessResponse confirmAndProcessPayment(Map<String, String> params) {
        // 파라미터 추출
        String orderId = params.get("orderId");
        String paymentKey = params.get("paymentKey");
        Integer amount = Integer.valueOf(params.get("amount"));

        // 금액 검증
//        if(!paymentService.validateOrderAmount(orderId, amount)){
//        log.error("결제금액과 주문금액이 다름");
//            //TODO: 결제 취소 로직 구현
//        }

        log.info("토스 결제 승인 시작 : orderId : {}\npaymentKey : {}\namount : {}", orderId, paymentKey, amount);

        // API 요청 객체 생성
        TossConfirmRequest req = new TossConfirmRequest(amount, orderId, "1");
        String authorization = buildAuthorizationHeader();

        // API 호출
        log.info("Toss Payments API 승인 요청");
        TossConfirmResponse tossConfirmResponse = tossPaymentsApiClient.confirmPayment(authorization, req);
        log.info("Toss Payments API 승인 성공");


        // 결제사 응답값 -> 공용 db 처리
        return new ConfirmSuccessResponse(
                tossConfirmResponse.paymentKey(),
                tossConfirmResponse.orderId(),
                tossConfirmResponse.totalAmount(),
                tossConfirmResponse.method(),
                tossConfirmResponse.status(),
                tossConfirmResponse.requestedAt(),
                tossConfirmResponse.receipt().url()
        );
    }

    private String buildAuthorizationHeader(){
        String encodeSecretKey = Base64.getEncoder().encodeToString((properties.getSecretKey() + ":").getBytes(
                StandardCharsets.UTF_8));
        return "Basic " + encodeSecretKey;
    }
}
