package com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartItemTest {

    @Test
    @DisplayName("updateQuantity: 정상 범위면 quantity가 변경된다")
    void updateQuantity_ok() {
        CartItem item = CartItem.builder()
                .bookId(1L)
                .quantity(1)
                .selected(true)
                .build();

        int newQuantity = CartConstants.MIN_QUANTITY; // 하한
        item.updateQuantity(newQuantity);

        assertThat(item.getQuantity()).isEqualTo(newQuantity);

        newQuantity = CartConstants.MAX_QUANTITY; // 상한
        item.updateQuantity(newQuantity);

        assertThat(item.getQuantity()).isEqualTo(newQuantity);
    }

    @Test
    @DisplayName("updateQuantity: 최소 미만이면 IllegalArgumentException 발생(커버리지: validate(false) 분기)")
    void updateQuantity_underMin_throw() {
        CartItem item = CartItem.builder()
                .bookId(1L)
                .quantity(1)
                .selected(true)
                .build();

        int invalid = CartConstants.MIN_QUANTITY - 1;

        assertThatThrownBy(() -> item.updateQuantity(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수량은 최소")
                .hasMessageContaining(String.valueOf(CartConstants.MIN_QUANTITY))
                .hasMessageContaining(String.valueOf(CartConstants.MAX_QUANTITY));
    }

    @Test
    @DisplayName("updateQuantity: 최대 초과면 IllegalArgumentException 발생(커버리지: validate(false) 분기)")
    void updateQuantity_overMax_throw() {
        CartItem item = CartItem.builder()
                .bookId(1L)
                .quantity(1)
                .selected(true)
                .build();

        int invalid = CartConstants.MAX_QUANTITY + 1;

        assertThatThrownBy(() -> item.updateQuantity(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수량은 최소")
                .hasMessageContaining(String.valueOf(CartConstants.MIN_QUANTITY))
                .hasMessageContaining(String.valueOf(CartConstants.MAX_QUANTITY));
    }
}
