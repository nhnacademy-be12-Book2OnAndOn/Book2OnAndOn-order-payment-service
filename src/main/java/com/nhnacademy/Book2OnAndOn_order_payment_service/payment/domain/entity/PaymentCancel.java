package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Column(name = "cancel_amount")
    @NotNull
    private Integer cancelAmount;

    @Column(name = "cancel_reason", length = 100)
    @Size(max = 100)
    @NotNull
    private String cancelReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt = LocalDateTime.now();

    @Column(name = "payment_transaction_key", length = 64, unique = true)
    private String paymentTransactionKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    // 생성 로직
    public PaymentCancel(PaymentCancelCreateRequest req) {
        this.cancelAmount = req.cancelAmount();
        this.cancelReason = req.cancelReason();
        this.canceledAt = Objects.isNull(req.canceledAt()) ? LocalDateTime.now() : req.canceledAt();
        this.paymentTransactionKey = req.paymentTransactionKey();

        payment.setPaymentId(req.paymentId());
        payment.getPaymentCancel().add(this);
    }
    
    // 비즈니스 로직
    public PaymentCancelResponse toResponse(){
        return new PaymentCancelResponse(
                this.paymentCancelId,
                this.payment.getPaymentId(),
                this.cancelAmount,
                this.cancelReason,
                this.canceledAt,
                this.paymentTransactionKey
        );
    }
}
