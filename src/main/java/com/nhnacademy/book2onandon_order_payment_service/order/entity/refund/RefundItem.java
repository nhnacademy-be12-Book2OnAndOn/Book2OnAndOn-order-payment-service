package com.nhnacademy.book2onandon_order_payment_service.order.entity.refund;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
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
@Table(name = "refund_item")
public class RefundItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_item_id")
    private Long refundItemId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @NotNull
    @Column(name = "refund_quantity", columnDefinition = "TINYINT")
    private Integer refundQuantity; // 반품한 수량 (ex. 3/5 ...)

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @NotNull
    @Column(name="original_order_item_status", columnDefinition="TINYINT")
    private Integer originalOrderItemStatus;

    // ===== 환불 전용 생성 책임 =====
    public static RefundItem create(Refund refund, OrderItem orderItem, int quantity) {
        if (refund == null) throw new IllegalArgumentException("refund는 필수입니다.");
        if (orderItem == null) throw new IllegalArgumentException("orderItem은 필수입니다.");
        if (quantity <= 0) throw new IllegalArgumentException("quantity는 1 이상이어야 합니다.");

        RefundItem ri = new RefundItem();
        ri.refund = refund;
        ri.orderItem = orderItem;
        ri.refundQuantity = quantity;
        if (orderItem.getOrderItemStatus() == null) {
            throw new IllegalStateException("orderItemStatus가 null이면 반품 원복을 보장할 수 없습니다.");
        }
        ri.originalOrderItemStatus = orderItem.getOrderItemStatus().getCode();
        return ri;
    }

    public OrderItemStatus getOriginalStatus() {
        return originalOrderItemStatus == null ? null : OrderItemStatus.fromCode(originalOrderItemStatus);
    }
}