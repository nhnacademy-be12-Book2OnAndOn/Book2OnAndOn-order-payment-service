package com.nhnacademy.Book2OnAndOn_order_payment_service.payment_service.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Payment")
public class Payment {
    @Id
    private String paymentKey;

    private Long orderId;         // 주문 참조

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    private PaymentProvider paymentProvider;

    private int paymentTotalAmount;

    private int paymentBalanceAmount;

    private OffsetDateTime paymentRequestAt;

    private OffsetDateTime paymentApprovedAt;

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaymentFailure paymentFailure;

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaymentCancel paymentCancel;

}