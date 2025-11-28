package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.ConfirmSuccessResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentStrategyFactory factory;

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

    // 결제 성공 및 검증 후 승인 요청(토스)
    @GetMapping("/{provider}/confirm")
    public ResponseEntity<PaymentResponse> successPaymentAndConfirm(@PathVariable("provider") String provider,
                                                                    @RequestParam Map<String, String> params){
        // 결제 제공사
        PaymentStrategy paymentStrategy = factory.getStrategy(provider);
        ConfirmSuccessResponse confirmSuccessResponse = paymentStrategy.confirmAndProcessPayment(params);

        // 공용 승인 응답 -> 결제 생성 요청
        PaymentCreateRequest req = new PaymentCreateRequest(
                confirmSuccessResponse.paymentKey(),
                confirmSuccessResponse.orderId(),
                confirmSuccessResponse.totalAmount(),
                confirmSuccessResponse.method(),
                paymentStrategy.getProvider(),
                confirmSuccessResponse.status(),
                confirmSuccessResponse.requestedAt(),
                confirmSuccessResponse.receiptUrl(),
                0
        );

        PaymentResponse resp = paymentService.createPayment(req);
        return ResponseEntity.ok(resp);
    }

    // 결제 실패

    // 결제 승인

}
