package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "toss-payments-api", url = "https://api.tosspayments.com")
public interface TossPaymentsApiClient {

    // 결제 승인 (실제로 돈이 빠져나가는 시점)
    @PostMapping(value = "/v1/payments/confirm", consumes = "application/json")
    TossResponse confirmPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TossConfirmRequest req
    );

    // 결제 취소 (결제 취소)
    @PostMapping(value = "/v1/payments/{paymentKey}/cancel", consumes = "application/json")
    TossCancelResponse cancelPayment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("paymentKey") String paymentKey,
            @RequestBody TossCancelRequest req
    );

    // 결제 조회 (orderId로 조회)
    @GetMapping(value = "/v1/payments/orders/{orderId}", consumes = "application/json")
    TossResponse findPayment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("orderId") String orderId
    );
}
