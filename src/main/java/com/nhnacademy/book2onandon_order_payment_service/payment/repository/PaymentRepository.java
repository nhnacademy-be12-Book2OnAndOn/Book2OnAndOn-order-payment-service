package com.nhnacademy.book2onandon_order_payment_service.payment.repository;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PaymentRepository extends JpaRepository<Payment, String> {
    // 최초 결제
    Optional<Payment> findByOrderNumber(String orderNumber);
}
