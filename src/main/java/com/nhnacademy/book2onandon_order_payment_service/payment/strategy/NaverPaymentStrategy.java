package com.nhnacademy.book2onandon_order_payment_service.payment.strategy;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.exception.NotSupportedPayments;
import org.springframework.stereotype.Component;

@Component
public class NaverPaymentStrategy implements PaymentStrategy{
    @Override
    public String getProvider() {
        return "NAVER";
    }

    @Override
    public CommonResponse confirmPayment(CommonConfirmRequest req, String idempotencyKey) {
        throw new NotSupportedPayments("현재 지원하지 않는 결제 서비스 입니다");
    }

    @Override
    public CommonCancelResponse cancelPayment(CommonCancelRequest req, String IdempotencyKey) {
        throw new NotSupportedPayments("현재 지원하지 않는 결제 서비스 입니다");
    }
}
