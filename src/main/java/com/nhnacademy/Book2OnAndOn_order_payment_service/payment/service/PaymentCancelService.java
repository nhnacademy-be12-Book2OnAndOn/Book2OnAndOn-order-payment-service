package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelListRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;

import java.util.List;

public interface PaymentCancelService {
    List<PaymentCancelResponse> getPaymentCancelList(PaymentCancelListRequest req);
    PaymentCancelResponse getPaymentCancel(PaymentCancelRequest req);
    PaymentCancelResponse createPaymentCancel(PaymentCancelCreateRequest req);
}
