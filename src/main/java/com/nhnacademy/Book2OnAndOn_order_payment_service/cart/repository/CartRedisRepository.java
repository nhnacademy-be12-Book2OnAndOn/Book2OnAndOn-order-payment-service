package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartRedisItem;
import java.util.Map;
import java.util.Set;

// Redis에 장바구니(임시 캐싱용)를 저장/조회하기 위한 규약(interface)
public interface CartRedisRepository {

    // ======================
    // 1. 회원 장바구니 (userId = Long)
    // ======================
    // 1) 장바구니 아이템 전체 조회
    Map<Long, CartRedisItem> getUserCartItems(Long userId);

    // 2) 회원 장바구니를 한 번에 채워넣기
    void putUserItem(Long userId, CartRedisItem cartRedisItem);

    // 3) merge 후 회원 장바구니 캐시 무효화
    void clearUserCart(Long userId);

    // 4) 회원 장바구니 단일 삭제 (write-behind용 추가)
    void deleteUserCartItem(Long userId, long bookId);

    // 5) 회원 장바구니 dirty set 관리
    void markUserCartDirty(Long userId);

    Set<Long> getDirtyUserIds();

    void clearUserCartDirty(Long userId);


    // ======================
    // 2. 비회원 장바구니 (uuid = String)
    // ======================
    // 1) 장바구니 아이템 전체 조회
    Map<Long, CartRedisItem> getGuestCartItems(String uuid);

    // 2) 장바구니 상태 갱신. TTL 연장
    void putGuestItem(String uuid, CartRedisItem item);

    // 3) 장바구니 항목 수량을 변경. TTL 연장
    void updateGuestItemQuantity(String uuid, long bookId, int quantity);

    // 4) 장바구니에서 특정 책 제거. TTL 연장
    void deleteGuestItem(String uuid, long bookId);

    // 5) 비회원 세션 종료나 로그인 후 병합 시, 비회원 카트 전체 삭제
    void clearGuestCart(String uuid);

}
