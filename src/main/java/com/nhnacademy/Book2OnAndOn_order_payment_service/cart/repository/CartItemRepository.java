package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.Cart;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 장바구니 내부의 개별 Item 데이터(DB) 를 조작하는 계층.
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 1. 장바구니의 전체 아이템 조회
    List<CartItem> findByCart(Cart cart);

    // 2. 카트아이템 전체 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CartItem ci where ci.cart = :cart")
    void deleteByCart(@Param("cart") Cart cart);

    // 3. 카트아이템 bookid로 일부 삭제
    void deleteByCartAndBookIdIn(Cart cart, List<Long> bookIds);

}
