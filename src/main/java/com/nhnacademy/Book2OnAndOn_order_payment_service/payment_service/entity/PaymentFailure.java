package com.nhnacademy.Book2OnAndOn_order_payment_service.payment_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결제 실패 이력 기록
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PaymentFailure")
public class PaymentFailure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long failureId;

    private String paymentKey;

    private String paymentErrorMessage;

    private String paymentErrorCode;
}