package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemSelectAllRequestDto {

    private boolean selected; // true: 전체 선택, false: 전체 해제
}
