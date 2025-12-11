package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refund")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @NotNull
    @Column(name = "refund_reason", length = 20)
    private String refundReason;

    @Column(name = "refund_reason_detail", length = 100)
    private String refundReasonDetail;

    @NotNull
    @Column(name = "refund_status", columnDefinition = "TINYINT")
    private Integer refundStatus;

    @NotNull
    @Column(name = "refund_created_at")
    private LocalDateTime refundCreatedAt = LocalDateTime.now();

    @NotNull
    @JoinColumn(name = "order_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL)
    private List<RefundItem> refundItem = new ArrayList<>();
}