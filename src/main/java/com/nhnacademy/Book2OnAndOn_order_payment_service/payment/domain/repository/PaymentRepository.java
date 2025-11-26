package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByOrderId(Long orderId);
}
