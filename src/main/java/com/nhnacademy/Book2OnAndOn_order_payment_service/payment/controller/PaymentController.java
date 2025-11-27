package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import jakarta.ws.rs.PATCH;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    // 주문 조회시 결제정보 리턴
    @GetMapping
    public ResponseEntity<PaymentResponse> getPayment(@RequestParam("orderNumber") String orderNumber){
        log.info("GET /payment 요청 수신 (주문번호 : {})", orderNumber);
        PaymentRequest req = new PaymentRequest(orderNumber);

        PaymentResponse resp = paymentService.getPayment(req);

        return ResponseEntity.ok(resp);
    }

    @DeleteMapping
    public ResponseEntity<PaymentDeleteResponse> deletePayment(@RequestParam("orderNumber") String orderNumber){
        log.info("DELETE /payment 요청 수신 (주문번호 : {})", orderNumber);
        PaymentDeleteRequest req = new PaymentDeleteRequest(orderNumber);

        PaymentDeleteResponse resp = paymentService.deletePayment(req);

        return ResponseEntity.ok(resp);
    }

}
