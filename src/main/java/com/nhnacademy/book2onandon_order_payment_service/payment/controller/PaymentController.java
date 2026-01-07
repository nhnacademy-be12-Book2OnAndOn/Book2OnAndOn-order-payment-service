package com.nhnacademy.book2onandon_order_payment_service.payment.controller;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    // 결제 성공 및 검증 후 승인 요청
    @PostMapping("/{provider}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable("provider") String provider,
                                                          @RequestBody CommonConfirmRequest req){
        log.info("POST /payment/{}/confirm 요청 수신 (주문번호 : {})", provider, req.orderId());

        PaymentResponse paymentResponse = paymentService.confirmAndCreatePayment(provider, req);

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponse);
    }
}
