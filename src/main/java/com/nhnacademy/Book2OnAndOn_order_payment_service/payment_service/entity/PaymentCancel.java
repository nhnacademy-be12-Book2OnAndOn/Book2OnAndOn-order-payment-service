package com.nhnacademy.Book2OnAndOn_order_payment_service.payment_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Payment_Cancel")
public class PaymentCancel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String paymentKey;       // 주문 참조

    private int paymentCancelAmount;

    private ZonedDateTime paymentCancelAt;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "payment_key")
    private Payment payment;
}