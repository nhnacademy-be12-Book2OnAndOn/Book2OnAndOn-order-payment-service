package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "Payment")
public class Payment {
    @Id
    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "order_number", length = 12)
    @NotNull
    private String orderNumber;

    @Column(name = "total_amount")
    @NotNull
    private Integer totalAmount;

    @Column(name = "payment_method")
    @NotNull
    private PaymentMethod paymentMethod;

    @Column(name = "payment_provider")
    @Enumerated(value = EnumType.STRING)
    @NotNull
    private PaymentProvider paymentProvider;

    @Column(name = "payment_status")
    @NotNull
    private PaymentStatus paymentStatus;

    @Column(name = "payment_created_at")
    @NotNull
    private LocalDateTime paymentCreatedAt = LocalDateTime.now();

    @Column(name = "payment_receipt_url")
    @Size(max = 200)
    private String paymentReceiptUrl;

    @Column(name = "refund_amount")
    private Integer refundAmount = 0;

    // 생성 로직
    public Payment(PaymentCreateRequest req){
        this.paymentKey = req.paymentKey();
        this.orderNumber = req.orderNumber();
        this.totalAmount = req.totalAmount();
        this.paymentMethod = PaymentMethod.fromExternal(req.paymentMethod());
        this.paymentProvider = PaymentProvider.valueOf(req.paymentProvider());
        this.paymentStatus = PaymentStatus.fromExternal(req.paymentStatus());
        if(req.paymentCreatedAt() != null){
            this.paymentCreatedAt = req.paymentCreatedAt();
        }

        this.paymentReceiptUrl = req.paymentReceiptUrl();

        if(req.refundAmount() != null){
            this.refundAmount = req.refundAmount();
        }
    }

    // 비즈니스 로직
    public PaymentResponse toResponse(){
        return new PaymentResponse(
                this.paymentKey,
                this.orderNumber,
                this.totalAmount,
                this.paymentMethod.getDescription(),
                this.paymentProvider.name(),
                this.paymentStatus.getDescription(),
                this.paymentCreatedAt,
                this.paymentReceiptUrl,
                this.refundAmount
        );
    }
}
