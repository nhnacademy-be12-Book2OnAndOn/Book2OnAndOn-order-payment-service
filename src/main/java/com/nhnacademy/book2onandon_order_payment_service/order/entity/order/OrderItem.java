package com.nhnacademy.book2onandon_order_payment_service.order.entity.order;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "OrderItem")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "book_id")
    @NotNull
    private Long bookId;

    @Column(name = "order_item_quantity", columnDefinition = "TINYINT")
    @NotNull
    private Integer orderItemQuantity = 1;

    @Column(name = "unit_price")
    @NotNull
    private Integer unitPrice;

    @Column(name = "is_wrapped")
    @NotNull
    private boolean isWrapped = false;

    @Column(name = "order_item_status", columnDefinition = "TINYINT")
    private OrderItemStatus orderItemStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @NotNull
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "wrapping_paper_id")
    private WrappingPaper wrappingPaper;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL)
    private List<RefundItem> refundItems = new ArrayList<>();

    public void updateStatus(OrderItemStatus status){
        this.orderItemStatus = status;
    }
}