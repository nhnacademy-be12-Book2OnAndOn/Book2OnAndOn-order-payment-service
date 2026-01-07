package com.nhnacademy.book2onandon_order_payment_service.payment.repository;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.PaymentCancel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, String> {
    List<PaymentCancel> findByPaymentKey(String paymentKey);
}
