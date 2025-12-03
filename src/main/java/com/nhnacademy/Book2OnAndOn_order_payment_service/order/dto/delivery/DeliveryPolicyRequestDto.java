package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPolicyRequestDto {
    private String deliveryPolicyName;
    private Integer deliveryFee;
    private Integer freeDeliveryThreshold;
}
