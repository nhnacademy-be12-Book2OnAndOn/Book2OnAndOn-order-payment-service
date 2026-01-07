package com.nhnacademy.book2onandon_order_payment_service.order.entity.refund;

import com.nhnacademy.book2onandon_order_payment_service.order.converter.RefundStatusConverter;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "refund",
        indexes = {
                @Index(name="idx_refund_order_id", columnList="order_id"),
                @Index(name="idx_refund_created_at", columnList="refund_created_at"),
                @Index(name="idx_refund_status", columnList="refund_status")
        }
)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @NotNull
    @JoinColumn(name = "order_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_reason", length = 20)
    private RefundReason refundReason;

    @Column(name = "refund_reason_detail", length = 255)
    private String refundReasonDetail;

    @Convert(converter = RefundStatusConverter.class)
    @Column(name="refund_status", columnDefinition="TINYINT", nullable=false)
    private RefundStatus refundStatus; // 컨버터가 enum<->tinyint 처리

    @NotNull
    @Column(name = "refund_created_at")
    private LocalDateTime refundCreatedAt;

    @Column(name = "original_order_status", columnDefinition = "TINYINT")
    private Integer originalOrderStatus;

    @Column(name = "shipping_deduction_amount")
    private Integer shippingDeductionAmount = 0;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true) // 부모 엔티티에서 고아가 된(Orphaned) 자식 엔티티를 자동으로 데이터베이스에서 제거(DELETE)
    private List<RefundItem> refundItems = new ArrayList<>();

    // ===== 환불 전용 연관관계 편의 메서드 =====
    public void addRefundItem(RefundItem refundItem) {
        if (refundItem == null) return;
        if (this.refundItems == null) {
            this.refundItems = new ArrayList<>();
        }
        this.refundItems.add(refundItem);
        refundItem.setRefund(this);
    }

    public OrderStatus getOriginalOrderStatusEnum() {
        if (originalOrderStatus == null) return null;
        return OrderStatus.fromCode(originalOrderStatus);
    }

    public static Refund create(Order order, RefundReason reason, String reasonDetail) {
        if (order == null) {
            throw new IllegalArgumentException("order는 필수입니다.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("refundReason은 필수입니다.");
        }

        Refund refund = new Refund();
        refund.order = order;
        refund.refundReason = reason;
        refund.refundReasonDetail = reasonDetail;
        refund.refundStatus = RefundStatus.REQUESTED;
        refund.refundCreatedAt = LocalDateTime.now();
        // shippingDeductionAmount는 완료 시점에 확정(REFUND_COMPLETED 처리 시 저장)
        return refund;
    }

}