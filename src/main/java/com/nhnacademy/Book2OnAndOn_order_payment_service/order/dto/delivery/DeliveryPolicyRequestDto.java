package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPolicyRequestDto {

    @NotBlank(message = "배송 정책 이름은 필수입니다.")
    private String deliveryPolicyName;

    @NotNull(message = "배송비는 필수입니다.")
    @Min(value = 0, message = "배송비는 0원 이상이어야 합니다.")
    private Integer deliveryFee;

    @NotNull(message = "무료 배송 기준 금액은 필수입니다.")
    @Min(value = 0, message = "무료 배송 기준 금액은 0원 이상이어야 합니다.")
    private Integer freeDeliveryThreshold;
}
