package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "PaymentFailure")
public class PaymentFailure {
    @Id
    private String id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private String code;
    private String message;

    public PaymentFailure(Payment payment, String code, String message) {
        this.payment = payment;
        this.code = code;
        this.message = message;
    }
}
