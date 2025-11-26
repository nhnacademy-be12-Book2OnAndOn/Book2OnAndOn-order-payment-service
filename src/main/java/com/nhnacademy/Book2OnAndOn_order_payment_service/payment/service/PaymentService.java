package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdatePaymentStatusRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse getPayment(PaymentRequest req);
    PaymentResponse createPayment(PaymentCreateRequest req);
    PaymentDeleteResponse deletePayment(PaymentDeleteRequest req);
    PaymentResponse updateRefundAmountPayment(PaymentUpdateRefundAmountRequest req, Integer refundAmount);
    PaymentResponse updatePaymentStatus(PaymentUpdatePaymentStatusRequest req);
}
