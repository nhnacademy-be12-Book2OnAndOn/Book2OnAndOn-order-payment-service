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
    void updateGuestItemQuantity(String uuid, long bookId, int quantity);
    // 3) 회원 장바구니 단일 삭제 (write-behind용 추가)
    void deleteUserCartItem(Long userId, long bookId);
    // 4) merge 후 회원 장바구니 캐시 무효화
    void clearUserCart(Long userId);


    // ======================
    // 2. 비회원 장바구니 (uuid = String)
    // ======================
    // 1) 장바구니 아이템 전체 조회
    Map<Long, CartRedisItem> getGuestCartItems(String uuid);

    // 2) 장바구니 상태 갱신. TTL 연장
    void putGuestItem(String uuid, CartRedisItem item);

    // 4) 장바구니에서 항목 삭제. TTL 연장
    void deleteGuestItem(String uuid, long bookId);

    // 5) 비회원 세션 종료나 로그인 후 병합 시, 비회원 카트 전체 삭제
    void clearGuestCart(String uuid);


    // ======================
    // 3. 장바구니 동기화 관리 (DB 반영/Write-behind용)
    // ======================
    // 1) Dirty Mark 등록 (사용자별 변경 여부)
    void markUserCartDirty(Long userId);

    // 2) 변경/삭제된 항목 추적
    void markUserItemDirty(Long userId, Long bookId);
    void markUserItemDeleted(Long userId, Long bookId);

    // 3) 동기화 대상 데이터 추출
    Set<Long> getDirtyUserIds();
    Set<Long> getDirtyItemIds(Long userId);
    Set<Long> getDeletedItemIds(Long userId);

    // 4) 상태 초기화 및 정리
    void clearUserCartDirty(Long userId);
    void markUserCartClear(Long userId);
    boolean consumeUserCartClear(Long userId);

    // (임시) dirty 키 정리
    void cleanupUserDirtyKeys(Long userId);

}
