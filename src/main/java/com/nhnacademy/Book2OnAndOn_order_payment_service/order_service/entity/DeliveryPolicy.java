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

/**
 * 배송 정책 기준 정보
 */
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

    // 무료 배송 기준 금액
    private int freeShippingThreshold;

    // 기본 배송 비용
    private int baseDeliveryFee;

    // 추가 배송 비용
    private int extraDeliveryFee;
    
    /** 적용 기간, 사용 여부 등 관리 필드 추가 가능 */
}