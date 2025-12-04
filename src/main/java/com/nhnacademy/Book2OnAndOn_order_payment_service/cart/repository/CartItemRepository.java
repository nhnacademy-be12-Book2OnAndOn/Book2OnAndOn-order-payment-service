package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.Cart;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// 장바구니 내부의 개별 Item 데이터(DB) 를 조작하는 계층.
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 1. 장바구니의 전체 아이템 조회
    List<CartItem> findByCart(Cart cart);

    // 2. 장바구니의 단일 아이템 조회 (Unique: cart + bookId)
    Optional<CartItem> findByCartAndBookId(Cart cart, Long bookId);

    // 3. 장바구니 안의 서로 다른 품목 개수 계산 (장바구니 사이즈 제한)
    long countByCart(Cart cart);

    // 4. 장바구니 안의 특정 아이템 1개를 삭제
    @Modifying
    void deleteByCartAndBookId(Cart cart, Long bookId);

    // 5. 선택된 모든 아이템 삭제
    @Modifying
    void deleteByCartAndSelectedTrue(Cart cart);

    // 6. 전체 선택/해제 bulk update
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CartItem ci set ci.selected = :selected where ci.cart = :cart")
    void updateSelectedAllByCart(@Param("cart") Cart cart,
                                 @Param("selected") boolean selected);
}
