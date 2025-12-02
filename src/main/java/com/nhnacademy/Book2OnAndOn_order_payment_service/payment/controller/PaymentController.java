package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // 결제 내역 삭제용
    @DeleteMapping
    public ResponseEntity<PaymentDeleteResponse> deletePayment(@RequestParam("orderNumber") String orderNumber){
        log.info("DELETE /payment 요청 수신 (주문번호 : {})", orderNumber);
        PaymentDeleteRequest req = new PaymentDeleteRequest(orderNumber);
        PaymentDeleteResponse resp = paymentService.deletePayment(req);

        return ResponseEntity.ok(resp);
    }

    // 결제 성공 및 검증 후 승인 요청
    @GetMapping("/{provider}/confirm")
    public ResponseEntity<PaymentResponse> successPaymentAndConfirm(@PathVariable("provider") String provider,
                                                                    @RequestParam Map<String, String> params){
        log.info("GET /payment/{}/confirm 요청 수신 (주문번호 : {})", provider, params.get("orderId"));
        // 결제 제공사
        PaymentStrategy paymentStrategy = factory.getStrategy(provider);

        // 파라미터 값 -> 공통 승인 요청 변환
        CommonConfirmRequest confirmReq = new CommonConfirmRequest(params.get("orderId"),
                params.get("paymentKey"),
                Integer.valueOf(params.get("amount"))
        );


        // 승인 요청
        CommonConfirmResponse confirmResp = paymentStrategy.confirmAndProcessPayment(confirmReq);

        // 공용 승인 응답 -> 결제 생성 요청 변환
        PaymentCreateRequest req = new PaymentCreateRequest(
                confirmResp.paymentKey(),
                confirmResp.orderId(),
                confirmResp.totalAmount(),
                confirmResp.method(),
                paymentStrategy.getProvider(),
                confirmResp.status(),
                confirmResp.requestedAt(),
                confirmResp.receiptUrl(),
                0
        );

        // TODO 결제 성공 핸들러 작성

        PaymentResponse resp = paymentService.createPayment(req);
        return ResponseEntity.ok(resp);
    }

    // 결제 취소 ( 사용자 / 관리자 ) 사용자 : 전체 취소, 관리자 : 부분 취소
    @PostMapping("/{provider}/cancel")
    public ResponseEntity<List<PaymentCancelResponse>> cancelPayment(@PathVariable("provider") String provider,
                                                                     @RequestBody CommonCancelRequest req,
                                                                     @RequestParam("orderNumber") String orderNumber){
        log.info("GET /payment/{}/cancel 요청 수신 (주문번호 : {})", provider, orderNumber);

        // 결제 제공사
        PaymentStrategy paymentStrategy = factory.getStrategy(provider);
        CommonCancelResponse resp = paymentStrategy.cancelPayment(req, orderNumber);

        PaymentCancelCreateRequest createRequest = new PaymentCancelCreateRequest(resp.paymentKey(),
                resp.status(), resp.cancels());

        List<PaymentCancelResponse> cancelResponse = paymentService.createPaymentCancel(createRequest);

        return ResponseEntity.ok(cancelResponse);
    }
}
