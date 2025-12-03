package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface PaymentRepository extends JpaRepository<Payment, String> {
    // 최초 결제
    Payment findByOrderNumber(String orderNumber);

    @Query("SELECT p.paymentProvider FROM Payment p WHERE p.orderNumber = :orderNumber")
    Optional<String> findProviderByOrderNumber(String orderNumber);
}
