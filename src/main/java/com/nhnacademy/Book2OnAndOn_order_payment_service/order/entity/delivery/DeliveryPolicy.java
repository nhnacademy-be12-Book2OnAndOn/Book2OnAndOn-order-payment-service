package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "DeliveryPolicy")
public class DeliveryPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_policy_id")
    private Long deliveryPolicyId;

    @Column(name = "delivery_policy_name", length = 20)
    @NotNull
    private String deliveryPolicyName;

    @Column(name = "delivery_fee")
    @NotNull
    private Integer deliveryFee;

    @Column(name = "free_delivery_threshold")
    @NotNull
    private Integer freeDeliveryThreshold;
    
}