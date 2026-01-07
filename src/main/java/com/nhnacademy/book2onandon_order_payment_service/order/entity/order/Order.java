package com.nhnacademy.book2onandon_order_payment_service.order.entity.order;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Table(name = "Orders")
public class Order {
    // 속성
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "order_number", unique = true, columnDefinition = "CHAR(15)")
    @NotNull
    private String orderNumber;

    @Column(name = "order_date_time")
    @NotNull
    @Builder.Default
    private LocalDateTime orderDateTime = LocalDateTime.now();

    @Column(name = "order_status")
    @NotNull
    private OrderStatus orderStatus;

    @Column(name = "order_title")
    private String orderTitle;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "total_discount_amount")
    private Integer totalDiscountAmount = 0;

    @Column(name = "total_item_amount")
    private Integer totalItemAmount;

    @Column(name = "delivery_fee")
    private Integer deliveryFee;

    @Column(name = "wrapping_fee")
    private Integer wrappingFee = 0;

    @Column(name = "coupon_discount")
    private Integer couponDiscount = 0;

    @Column(name = "point_discount")
    private Integer pointDiscount = 0;

    @Column(name = "want_delivery_date")
    private LocalDate wantDeliveryDate;

    // 양방향 연관관계
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeliveryAddress deliveryAddress;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private GuestOrder guestOrder;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Refund> refunds = new ArrayList<>();

    public void updateStatus(OrderStatus status){
        this.orderStatus = status;
    }

    public void addOrderItem(List<OrderItem> items){
        for (OrderItem item : items) {
            this.orderItems.add(item);
            item.setOrder(this);
        }
    }

    public void addDeliveryAddress(DeliveryAddress deliveryAddress){
        this.deliveryAddress = deliveryAddress;
        deliveryAddress.setOrder(this);
    }

    public void addRefund(Refund refund) {
        if (refund == null) {
            return;
        }
        if (this.refunds == null) {
            this.refunds = new ArrayList<>();
        }
        this.refunds.add(refund);
        refund.setOrder(this);
    }
}