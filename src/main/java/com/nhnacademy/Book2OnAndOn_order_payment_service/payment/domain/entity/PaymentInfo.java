package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.domain.entity.Order;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_info_id")
    private Long paymentInfoId;

    @Column(name = "payment_key", length = 200)
    @Size(max = 200)
    private String paymentKey;

    @Column(name = "total_amount")
    @NotNull
    private Integer totalAmount;

    @Column(name = "total_discount_amount")
    @NotNull
    private Integer totalDiscountAmount;

    @Column(name = "total_item_amount")
    @NotNull
    private Integer totalItemAmount;

    @Column(name = "delivery_fee")
    @NotNull
    private Integer deliveryFee;

    @Column(name = "wrapping_fee")
    @NotNull
    private Integer wrappingFee;

    @Column(name = "coupon_discount")
    @NotNull
    private Integer couponDiscount;

    @Column(name = "point_discount")
    @NotNull
    private Integer pointDiscount;

    @Column(name = "refund_amount")
    private Integer refundAmount;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @NotNull
    private Order order;
}
