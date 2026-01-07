package com.nhnacademy.book2onandon_order_payment_service.cart.repository;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 장바구니 자체(메타 정보) 에 대한 DB 접근 계층
public interface CartRepository extends JpaRepository<Cart, Long> {

    // 1. 해당 회원의 Cart 한 개를 조회
    Optional<Cart> findByUserId(Long userId);
}

