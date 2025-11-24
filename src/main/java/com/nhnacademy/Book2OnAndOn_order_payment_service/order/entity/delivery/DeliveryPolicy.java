package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "DeliveryPolicy")
public class DeliveryPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryPolicyId;

    @Column(name = "delivery_policy_name", length = 20, nullable = false)
    private String deliveryPolicyName;

    @Column(name = "delivery_cost", nullable = false)
    private int deliveryCost;

    @Column(name = "free_delivery_threshold", nullable = false)
    private int freeDeliveryThreshold;
    
}