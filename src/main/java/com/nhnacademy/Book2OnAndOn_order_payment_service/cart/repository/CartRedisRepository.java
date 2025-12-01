package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartRedisItem;
import java.util.Map;

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

    // ======================
    // 2. 비회원 장바구니 (uuid(guestUuid) = String)
    // ======================
    // 1) 장바구니 아이템 전체 조회
    Map<Long, CartRedisItem> getGuestCartItems(String guestUuid);

    // 2) 장바구니에 책 1권 추가 (이미 있으면 quantity +1). TTL 연장
//    void addGuestItem(String guestUuid, long bookId);

    // 3) 장바구니 항목 수량을 지정 값으로 변경 (증감이 아닌 "최종 값"). TTL 연장
    void updateGuestItemQuantity(String guestUuid, long bookId, int quantity, long updatedAt);

    // 4) 장바구니 항목의 선택/수량 등 전체 속성 갱신 (TTL 연장)
    void updateGuestItem(String guestUuid, CartRedisItem item);

    // 5) 장바구니에서 특정 책 제거. TTL 연장
    void deleteGuestItem(String guestUuid, long bookId);

    // 6) 비회원 세션 종료나 로그인 후 병합 시, 비회원 카트 전체 삭제
    void clearGuestCart(String guestUuid);

}
