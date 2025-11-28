package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelListRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, String> {
    @Query("SELECT pc FROM PaymentCancel pc JOIN FETCH pc.payment p WHERE p.paymentId = :paymentId")
    List<PaymentCancel> findByPaymentIdWithPayment(String paymentId);
}
