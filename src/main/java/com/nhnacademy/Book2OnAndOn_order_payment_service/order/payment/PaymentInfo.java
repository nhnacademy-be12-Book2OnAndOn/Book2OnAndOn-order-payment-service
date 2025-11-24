package com.nhnacademy.Book2OnAndOn_order_payment_service.order.payment;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor // Lombok: 기본 생성자
@AllArgsConstructor // Lombok: 모든 필드를 받는 생성자
@Table(name = "PaymentInfo")
public class PaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentInfoId;

    @Column(name = "toss_payment_key", length = 200, unique = true)
    private String tossPaymentKey;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Column(name = "total_discount_price", nullable = false)
    private int totalDiscountPrice;

    @Column(name = "total_item_price", nullable = false)
    private int totalItemPrice;

    @Column(name = "delivery_price", nullable = false)
    private int deliveryPrice;

    @Column(name = "wrapping_price", nullable = false)
    private int wrappingPrice;

    @Column(name = "cancel_price")
    private Integer cancelPrice;

    @Column(name = "refund_price")
    private Integer refundPrice;

    @Column(name = "payment_method", length = 20, nullable = false)
    private String paymentMethod;

    @Column(name = "payment_provider", length = 30, nullable = false)
    private String paymentProvider;

    @Column(name = "payment_status", columnDefinition = "TINYINT", nullable = false)
    private Integer paymentStatus;

    @Column(name = "payment_datetime", nullable = false)
    private LocalDateTime paymentDatetime;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}