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

    // 3) 회원 장바구니 단일 삭제 (write-behind용 추가)
    void deleteUserCartItem(Long userId, long bookId);

    // 4) merge 후 회원 장바구니 캐시 무효화
    void clearUserCart(Long userId);

    void putUserItems(Long userId, Map<Long, CartRedisItem> items);


    // ======================
    // 2. 비회원 장바구니 (uuid = String)
    // ======================
    // 1) 장바구니 아이템 전체 조회
    Map<Long, CartRedisItem> getGuestCartItems(String uuid);

    // 2) 장바구니 상태 갱신. TTL 연장
    void putGuestItem(String uuid, CartRedisItem item);
    void updateGuestItemQuantity(String uuid, long bookId, int quantity);

    // 4) 장바구니에서 항목 삭제. TTL 연장
    void deleteGuestItem(String uuid, long bookId);

    // 5) 비회원 세션 종료나 로그인 후 병합 시, 비회원 카트 전체 삭제
    void clearGuestCart(String uuid);

    void putGuestItems(String uuid, Map<Long, CartRedisItem> items);


    // ======================
    // 3. 장바구니 동기화 관리 (DB 반영/Write-behind용)
    // ======================
    // 1) Dirty Mark 등록 (사용자별 변경 여부)
    void markUserCartDirty(Long userId);

    // 2) 동기화 대상 데이터 추출
    Set<Long> getDirtyUserIds();

    // 3) 상태 초기화 및 정리
    void clearUserCartDirty(Long userId);

    boolean isUserCartDirty(Long userId);

}
