package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity;

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
@Table(name = "Delivery_policy")
public class DeliveryPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    private String policyName;

    private int freeShippingThreshold;

    private int baseDeliveryFee;

    private int extraDeliveryFee;
    
}