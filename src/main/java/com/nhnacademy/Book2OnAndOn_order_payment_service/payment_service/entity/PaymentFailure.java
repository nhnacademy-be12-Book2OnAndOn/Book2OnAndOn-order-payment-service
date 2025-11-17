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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PaymentFailure")
public class PaymentFailure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String paymentKey;

    private String paymentErrorMessage;

    private String paymentErrorCode;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "payment_key")
    private Payment payment;
}