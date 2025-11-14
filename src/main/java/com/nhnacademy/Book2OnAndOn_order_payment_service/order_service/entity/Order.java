package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 정보 엔티티
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Long userId;  //회원참조

    private LocalDateTime orderAt;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private int totalBookPrice;

    private int totalDiscount;

    private int finalPaymentAmount; // (totalBookPrice - totalDiscount + deliveryCost)

    private String receiver;

    private LocalDateTime wantDelivery;

    private String deliveryMessage;

    private int deliveryCost;
    /** OrderItem 리스트, Delivery 등 연관관계 매핑 추가필요 */
}