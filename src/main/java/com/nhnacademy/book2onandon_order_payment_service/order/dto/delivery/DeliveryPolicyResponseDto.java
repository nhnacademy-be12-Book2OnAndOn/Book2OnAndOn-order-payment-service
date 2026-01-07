package com.nhnacademy.book2onandon_order_payment_service.order.dto.delivery;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryPolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryPolicyResponseDto {
    private Long deliveryPolicyId;
    private String deliveryPolicyName;
    private Integer deliveryFee;
    private Integer freeDeliveryThreshold;

    public DeliveryPolicyResponseDto(DeliveryPolicy policy) {
        this.deliveryPolicyId = policy.getDeliveryPolicyId();
        this.deliveryPolicyName = policy.getDeliveryPolicyName();
        this.deliveryFee = policy.getDeliveryFee();
        this.freeDeliveryThreshold = policy.getFreeDeliveryThreshold();
    }
}
