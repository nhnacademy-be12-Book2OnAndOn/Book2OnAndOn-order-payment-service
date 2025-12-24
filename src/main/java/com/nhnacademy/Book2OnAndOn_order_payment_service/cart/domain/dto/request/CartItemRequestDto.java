package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequestDto {

    @NotNull
    private Long bookId;

    @Min(1)
    @Max(99)
    private int quantity;

    private boolean selected;
}
