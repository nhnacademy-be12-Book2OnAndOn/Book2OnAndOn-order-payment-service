package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.service.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.repository.PaymentRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponse getPayment(PaymentRequest req) {
        Payment payment = paymentRepository.findByOrderId(req.orderId());
        return null;
    }

    @Override
    public PaymentResponse createPayment(PaymentCreateRequest req) {
        return null;
    }
}
