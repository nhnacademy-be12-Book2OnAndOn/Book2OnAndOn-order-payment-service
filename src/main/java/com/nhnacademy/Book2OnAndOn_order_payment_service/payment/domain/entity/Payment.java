package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
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
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "total_amount")
    @NotNull
    private Integer totalAmount;

    @Column(name = "payment_method")
    @Size(max = 20)
    @NotNull
    private String paymentMethod;

    @Column(name = "payment_provider")
    @Size(max = 30)
    @NotNull
    private String paymentProvider;

    @Column(name = "payment_status")
    @NotNull
    private Byte paymentStatus;

    @Column(name = "payment_datetime")
    @NotNull
    private LocalDateTime paymentDatetime;

    @Column(name = "payment_receipt_url")
    @Size(max = 200)
    private String paymentReceiptUrl;

    @Column(name = "payment_key", length = 200, unique = true)
    @Size(max = 200)
    private String paymentKey;

    @Column(name = "refund_amount")
    private Integer refundAmount;

    @Column(name = "order_id")
    @NotNull
    private Long orderId;

    @OneToMany(mappedBy = "payment", orphanRemoval = true)
    private List<PaymentCancel> paymentCancel = new ArrayList<>();
}
