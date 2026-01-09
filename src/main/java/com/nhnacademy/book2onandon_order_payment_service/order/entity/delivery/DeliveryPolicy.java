package com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.delivery.DeliveryPolicyRequestDto;
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
@Table(name = "delivery_policy")
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

    public DeliveryPolicy(String deliveryPolicyName, Integer deliveryFee, Integer freeDeliveryThreshold) {
        this.deliveryPolicyName = deliveryPolicyName;
        this.deliveryFee = deliveryFee;
        this.freeDeliveryThreshold = freeDeliveryThreshold;
    }


    //정책 update
    public void update(DeliveryPolicyRequestDto requestDto) {
        this.deliveryPolicyName = requestDto.getDeliveryPolicyName();
        this.deliveryFee = requestDto.getDeliveryFee();
        this.freeDeliveryThreshold = requestDto.getFreeDeliveryThreshold();
    }

    public int calculateDeliveryFee(int totalItemAmount){
        if(this.freeDeliveryThreshold <= totalItemAmount){
            return 0;
        }
        return deliveryFee;
    }
}