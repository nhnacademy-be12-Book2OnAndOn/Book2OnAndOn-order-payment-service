package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {
}
