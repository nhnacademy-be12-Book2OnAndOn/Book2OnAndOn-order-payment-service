package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse getPayment(PaymentRequest req);
    PaymentResponse createPayment(PaymentCreateRequest req);
}
