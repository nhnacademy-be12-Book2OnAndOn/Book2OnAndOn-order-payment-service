package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.Cart;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 장바구니 내부의 개별 Item 데이터(DB) 를 조작하는 계층.
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 1. 장바구니의 전체 아이템 조회
    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByCartAndBookId(Cart cart, Long bookId);

    void deleteByCartAndBookIdIn(Cart cart, List<Long> bookIds);

}
