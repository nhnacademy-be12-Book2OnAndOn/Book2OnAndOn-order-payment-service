package com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartMergeStatusResponseDto {

    // 현재 Guest(uuid)에 연결된 Redis 장바구니가 존재하는가?
    private boolean hasGuestCart;
    // 로그인한 User에 연결된 DB (또는 Redis) 장바구니가 존재하는가?
    private boolean hasUserCart;
    // 비회원 장바구니에 몇 개의 아이템이 있는지 (모달에 표시할 숫자).
    private int guestItemCount;

}