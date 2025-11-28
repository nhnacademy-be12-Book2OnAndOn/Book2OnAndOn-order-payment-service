package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "toss-payments-api", url = "https://api.tosspayments.com")
public interface TossPaymentsApiClient {

    @PostMapping(value = "/v1/payments/confirm", consumes = "application/json")
    TossConfirmResponse confirmPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestBody TossConfirmRequest req
    );

    @PostMapping(value = "/v1/payments/{paymentKey}/cancel", consumes = "application/json")
    TossCancelResponse cancelPayment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("paymentKey") String paymentKey,
            @RequestBody TossCancelRequest req
    );
}
