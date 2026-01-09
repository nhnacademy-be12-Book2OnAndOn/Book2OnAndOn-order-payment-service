package com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDeleteRequestDto {

    @NotNull
    private Long bookId;
}

