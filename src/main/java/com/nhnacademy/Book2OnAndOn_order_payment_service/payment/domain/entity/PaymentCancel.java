package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

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
@Table(name = "PaymentCancel")
public class PaymentCancel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_cancel_id")
    private Long paymentCancelId;

    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "cancel_amount")
    @NotNull
    private Integer cancelAmount;

    @Column(name = "cancel_reason", length = 100)
    @Size(max = 100)
    @NotNull
    private String cancelReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt = LocalDateTime.now();



    // 생성 로직
    public PaymentCancel(String paymentKey, Integer cancelAmount, String cancelReason, LocalDateTime canceledAt) {
        this.paymentKey = paymentKey;
        this.cancelAmount = cancelAmount;
        this.cancelReason = cancelReason;
        this.canceledAt = Objects.isNull(canceledAt) ? LocalDateTime.now() : canceledAt;
    }
    
    // 비즈니스 로직
    public PaymentCancelResponse toResponse(){
        return new PaymentCancelResponse(
                this.paymentKey,
                this.cancelAmount,
                this.cancelReason,
                this.canceledAt
        );
    }
}
