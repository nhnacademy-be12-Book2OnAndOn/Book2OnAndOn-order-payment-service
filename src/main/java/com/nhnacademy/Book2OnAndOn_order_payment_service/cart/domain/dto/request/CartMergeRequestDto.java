package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartMergeRequestDto {

    @NotBlank
    private String guestUuid;
}
