package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdatePaymentStatusRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import java.util.List;


public interface PaymentService {
    PaymentResponse getPayment(PaymentRequest req);
    PaymentResponse createPayment(PaymentCreateRequest req);
    PaymentDeleteResponse deletePayment(PaymentDeleteRequest req);
    PaymentResponse updateRefundAmount(PaymentUpdateRefundAmountRequest req);
    PaymentResponse updatePaymentStatus(PaymentUpdatePaymentStatusRequest req);
    String getProvider(String orderNumber);

    // 결제 취소
    List<PaymentCancelResponse> createPaymentCancel(PaymentCancelCreateRequest req);
    List<PaymentCancelResponse> getCancelPaymentList(String paymentKey);
}
