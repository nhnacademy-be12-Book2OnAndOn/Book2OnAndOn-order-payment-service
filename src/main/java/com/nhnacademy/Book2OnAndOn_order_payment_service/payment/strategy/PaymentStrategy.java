package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;

public interface PaymentStrategy {
    String getProvider();
    CommonResponse confirmPayment(CommonConfirmRequest req, String idempotencyKey);
    CommonCancelResponse cancelPayment(CommonCancelRequest req, String orderNumber);
//    CommonResponse findPayment(String orderNumber);
}
