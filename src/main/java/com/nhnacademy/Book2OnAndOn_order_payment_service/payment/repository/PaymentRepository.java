package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 최초 결제
    Payment findByOrderNumber(String orderNumber);
    // 최신 결제(반품 / 취소)
//    Payment findTopByOrderNumberOrderByPaymentCreatedAtDesc(String orderNumber);

}
