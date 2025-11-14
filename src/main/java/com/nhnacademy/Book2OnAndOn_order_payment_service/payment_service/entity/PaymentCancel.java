package com.nhnacademy.Book2OnAndOn_order_payment_service.payment_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 결제 취소 이력을 기록
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Payment_Cancel")
public class PaymentCancel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cancelId;

    // 주문 참조
    private String paymentKey;

    private int paymentCancelAmount;

    private ZonedDateTime paymentCancelAt;
}