package com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsePointRequestDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long userId;
    @NotNull
    private Long orderId;
    @NotNull
    private Integer useAmount; // 사용할 포인트
    @NotNull
    private Integer allowedMaxUseAmount; // 최대 가용 가능 포인트
}
