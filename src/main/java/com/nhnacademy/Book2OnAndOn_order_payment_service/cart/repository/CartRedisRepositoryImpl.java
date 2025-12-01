package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartRedisItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.*;

// "Redis 장바구니 저장소”를 직접 다루는(Redis에 어떻게 저장·조회·삭제할지) 구현체
// Spring Data JPA는 자동 구현을 제공하지만 Redis는 자동 Repository 구현을 제공하지 않기 때문에 직접 인터페이스 + 구현체(Impl)를 만든 것.
@RequiredArgsConstructor
@Repository
public class CartRedisRepositoryImpl implements CartRedisRepository {

    // redisTemplate은 Redis 서버 연결을 자동으로 관리한다.
    private final RedisTemplate<String, CartRedisItem> redisTemplate;

    // ======================
    // 0. Redis 기본 설정
    // ======================
    // 1. Hash 타입으로 저장된 장바구니 데이터에 접근
    private HashOperations<String, Long, CartRedisItem> hashOps() {
        return redisTemplate.opsForHash();
    }

    // 2. 저장소 키를 결정
    private String userKey(Long userId) {
        return USER_CART_KEY_PREFIX + userId;
    }
    private String guestKey(String guestUuid) {
        return GUEST_CART_KEY_PREFIX + guestUuid;
    }

    // 3. 수량(quantity)을 1~99 범위 안으로 강제하는 로직
    private int capQuantity(int quantity) {
        return Math.max(MIN_QUANTITY, Math.min(quantity, MAX_QUANTITY));
    }

    // 4. 비회원 장바구니의 TTL을 갱신하는 기능
    private void extendGuestTtl(String guestUuid) {
        String key = guestKey(guestUuid);
        Map<Long, CartRedisItem> items = hashOps().entries(key);
        if (items == null || items.isEmpty()) {
            return; // 장바구니 자체가 없음
        }
//        // 30일 넘었으면 TTL 연장하지 않고 장바구니 삭제
//        LocalDateTime createdAt = items.values().iterator().next().getCreatedAt();
//
//        if (createdAt.plusDays(30).isBefore(LocalDateTime.now())) {
//            redisTemplate.delete(key);
//            return;
//        }
        redisTemplate.expire(key, Duration.ofHours(GUEST_CART_TTL_HOURS));
    }



    // ======================
    // 1. 회원 장바구니
    // ======================
    // 1) Redis 캐시에 저장된 회원 장바구니를 한 번에 불러옴
    @Override
    public Map<Long, CartRedisItem> getUserCartItems(Long userId) {
        Map<Long, CartRedisItem> map = hashOps().entries(userKey(userId));
        return map != null ? map : Collections.emptyMap(); // 비어있는 불변 Map 객체
    }

    // 2) DB → Redis 캐시로 복사
    @Override
    public void putUserItem(Long userId, CartRedisItem cartRedisItem) {
        if (userId == null || cartRedisItem == null || cartRedisItem.getBookId() == null) {
            throw new IllegalArgumentException("사용자 ID와 장바구니 항목 정보는 반드시 제공되어야 합니다.");
        }
        String key = userKey(userId);
        cartRedisItem.setQuantity(capQuantity(cartRedisItem.getQuantity()));
        hashOps().put(key, cartRedisItem.getBookId(), cartRedisItem);
    }

    // 3) 회원 장바구니를 DB에서 수정(담기, 변경, 삭제)할 때마다 호출
    @Override
    public void clearUserCart(Long userId) {
        redisTemplate.delete(userKey(userId));
    }


    // ======================
    // 2. 비회원 장바구니
    // ======================
    // 1) 전체 조회
    @Override
    public Map<Long, CartRedisItem> getGuestCartItems(String guestUuid) {
        Map<Long, CartRedisItem> map = hashOps().entries(guestKey(guestUuid));
        return map != null ? map : Collections.emptyMap();
    }

//    // 2) 아이템 추가
//    @Override
//    public void addGuestItem(String guestUuid, long bookId) {
//        String key = guestKey(guestUuid);
//        CartRedisItem current = hashOps().get(key, bookId);
//
//        if (current == null) {
//            CartRedisItem newItem = new CartRedisItem(bookId, 1, true);
////            newItem.setCreatedAt(LocalDateTime.now());
//            hashOps().put(key, bookId, newItem);
//        } else {
//            current.setSelected(true);
//            current.setQuantity(capQuantity(current.getQuantity() + 1));
//            hashOps().put(key, bookId, current);
//        }
//
//        extendGuestTtl(guestUuid);
//    }

    // 3) 수량 변경
    @Override
    public void updateGuestItemQuantity(String guestUuid, long bookId, int quantity, long updatedAt) {
        String key = guestKey(guestUuid);
        int capped = capQuantity(quantity);
        CartRedisItem current = hashOps().get(key, bookId);

        if (current == null) {
            hashOps().put(key, bookId, new CartRedisItem(bookId, capped, true, updatedAt));
        } else {
            current.setQuantity(capped);
            hashOps().put(key, bookId, current);
        }

        extendGuestTtl(guestUuid);
    }

    // 4) 전체 선택/해제
    @Override
    public void updateGuestItem(String guestUuid, CartRedisItem item) {
        if (item == null || item.getBookId() == null) {
            throw new IllegalArgumentException("item/bookId을 찾을 수 없습니다.");
        }

        item.setQuantity(capQuantity(item.getQuantity()));
        hashOps().put(guestKey(guestUuid), item.getBookId(), item);
        extendGuestTtl(guestUuid);
    }

    // 5) 단일 삭제
    @Override
    public void deleteGuestItem(String guestUuid, long bookId) {
        hashOps().delete(guestKey(guestUuid), bookId);
        extendGuestTtl(guestUuid);
    }

    // 6) 전체 삭제 (로그인 후 병합 완료, 주문 완료, 세션/UUID 만료 등..)
    @Override
    public void clearGuestCart(String guestUuid) {
        redisTemplate.delete(guestKey(guestUuid));
    }
}
