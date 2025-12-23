package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartRedisItem;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DIRTY_USERS = USER_CART_DIRTY_SET_KEY;
    private static final String DIRTY_ITEMS_PREFIX = "cart-service:user:dirty:items:";
    private static final String DIRTY_DELETED_PREFIX = "cart-service:user:dirty:deleted:";
    private static final String DIRTY_CLEAR_PREFIX = "cart-service:user:dirty:clear:";

    private String dirtyItemsKey(Long userId) { return DIRTY_ITEMS_PREFIX + userId; }
    private String dirtyDeletedKey(Long userId) { return DIRTY_DELETED_PREFIX + userId; }
    private String dirtyClearKey(Long userId) { return DIRTY_CLEAR_PREFIX + userId; }

    // ======================
    // 0. Redis 기본 설정
    // ======================
    // 1) Hash 타입으로 저장된 장바구니 데이터에 접근
    private HashOperations<String, Long, CartRedisItem> hashOps() {
        return redisTemplate.opsForHash();
    }

    // 2) 저장소 키를 결정
    private String userKey(Long userId) {
        return USER_CART_KEY_PREFIX + userId;
    }
    private String guestKey(String uuid) { return GUEST_CART_KEY_PREFIX + uuid; }

    // 3) 수량(quantity)을 min~max 범위 안으로 강제하는 로직
    private int capQuantity(int quantity) {
        return Math.max(MIN_QUANTITY, Math.min(quantity, MAX_QUANTITY));
    }

    // 4) 비회원 장바구니의 TTL을 갱신하는 기능
    private void extendGuestTtl(String uuid) {
        String key = guestKey(uuid);
        Map<Long, CartRedisItem> items = hashOps().entries(key);
        if (items == null || items.isEmpty()) {
            return; // 장바구니 자체가 없음
        }

        // 현재 시간 및 생성 시간 조회
        long now = System.currentTimeMillis();
        long createdAt = items.values().iterator().next().getCreatedAt();
        // 절대 수명 검사
        if (createdAt != 0) {
            long maxLifetimeMillis = GUEST_CART_MAX_LIFETIME_DAYS * 24L * 60L * 60L * 1000L;
            if (createdAt + maxLifetimeMillis < now) {
                // 생성 후 7일이 지나면 TTL 연장 없이 cart 다 날림
                redisTemplate.delete(key);
                return;
            }
        }
        // 활동 기반 수명 연장 (createdAt이 0이면 그냥 TTL만 연장)
        redisTemplate.expire(key, Duration.ofHours(GUEST_CART_TTL_HOURS));
    }


    // ======================
    // 1. 회원 장바구니
    // ======================
    // 1) 캐시 조회 : Redis 캐시에 저장된 회원 장바구니를 한 번에 불러옴
    @Override
    public Map<Long, CartRedisItem> getUserCartItems(Long userId) {
        Map<Long, CartRedisItem> map = hashOps().entries(userKey(userId));
        return map != null ? map : Collections.emptyMap(); // 비어있는 불변 Map 객체
    }

    // 2) 캐시 채워넣기 : DB → Redis 캐시로 복사
    @Override
    public void putUserItem(Long userId, CartRedisItem cartRedisItem) {
        if (userId == null || cartRedisItem == null || cartRedisItem.getBookId() == null) {
            throw new IllegalArgumentException("사용자 ID와 장바구니 항목 정보는 반드시 제공되어야 합니다.");
        }
        String key = userKey(userId);
        cartRedisItem.setQuantity(capQuantity(cartRedisItem.getQuantity())); // 수량이 정해진 최대/최소 허용 범위를 벗어나지 않도록

        // cart 레벨 createdAt 유지: 기존 아이템이 있으면 createdAt 재사용
        Map<Long, CartRedisItem> existingItems = hashOps().entries(key);
        if (existingItems != null && !existingItems.isEmpty()) {
            long createdAt = existingItems.values().iterator().next().getCreatedAt();
            if (createdAt > 0) {
                cartRedisItem.setCreatedAt(createdAt);
            }
        } else if (cartRedisItem.getCreatedAt() == 0) {
            long now = System.currentTimeMillis();
            cartRedisItem.setCreatedAt(now);
            cartRedisItem.setUpdatedAt(now);
        }

        hashOps().put(key, cartRedisItem.getBookId(), cartRedisItem);
    }

    // 3) 캐시 단일 삭제 : 회원 장바구니에서 특정 상품을 삭제
    @Override
    public void deleteUserCartItem(Long userId, long bookId) {
        hashOps().delete(userKey(userId), bookId);
    }

    // 4) 캐시 무효화 : Merge, flush 후 Redis 캐시 삭제(리셋)
    @Override
    public void clearUserCart(Long userId) {
        redisTemplate.delete(userKey(userId));
    }

    // ======================
    // 2. 비회원 장바구니
    // ======================
    // 1) 전체 조회
    @Override
    public Map<Long, CartRedisItem> getGuestCartItems(String uuid) {
        Map<Long, CartRedisItem> map = hashOps().entries(guestKey(uuid));
        return map != null ? map : Collections.emptyMap();
    }

    // 2) 상태 갱신
    @Override
    public void putGuestItem(String uuid, CartRedisItem cartRedisItem) {
        if (cartRedisItem == null || cartRedisItem.getBookId() == null) {
            throw new IllegalArgumentException("item/bookId을 찾을 수 없습니다.");
        }

        String key = guestKey(uuid);
        cartRedisItem.setQuantity(capQuantity(cartRedisItem.getQuantity()));

        // cart 레벨 createdAt 유지
        Map<Long, CartRedisItem> existingItems = hashOps().entries(key);
        if (existingItems != null && !existingItems.isEmpty()) {
            long createdAt = existingItems.values().iterator().next().getCreatedAt();
            if (createdAt > 0) {
                cartRedisItem.setCreatedAt(createdAt);
            }
        } else if (cartRedisItem.getCreatedAt() == 0) {
            long now = System.currentTimeMillis();
            cartRedisItem.setCreatedAt(now);
            cartRedisItem.setUpdatedAt(now);
        }

        hashOps().put(guestKey(uuid), cartRedisItem.getBookId(), cartRedisItem);
        extendGuestTtl(uuid);
    }

    // 3) 수량 변경
    @Override
    public void updateGuestItemQuantity(String uuid, long bookId, int quantity) {
        String key = guestKey(uuid);
        int capped = capQuantity(quantity);

        Map<Long, CartRedisItem> existingItems = hashOps().entries(key);
        long now = System.currentTimeMillis();
        long createdAt = now;

        if (existingItems != null && !existingItems.isEmpty()) {
            long existingItemCreatedAt = existingItems.values().iterator().next().getCreatedAt();
            if (existingItemCreatedAt > 0) {
                createdAt = existingItemCreatedAt;
            }
        }

        CartRedisItem current = hashOps().get(key, bookId);
        if (current == null) {
            current = new CartRedisItem(bookId, capped, true, createdAt, now);
        } else {
            current.setQuantity(capped);
            current.setUpdatedAt(now);
        }
        hashOps().put(key, bookId, current);
        extendGuestTtl(uuid);
    }

    // 4) 단일 삭제
    @Override
    public void deleteGuestItem(String uuid, long bookId) {
        hashOps().delete(guestKey(uuid), bookId);
        extendGuestTtl(uuid);
    }

    // 5) 전체 삭제(리셋) (로그인 후 병합 완료, 주문 완료, 세션/UUID 만료 등..)
    @Override
    public void clearGuestCart(String uuid) {
        redisTemplate.delete(guestKey(uuid));
    }


    // ======================
    // 3. 장바구니 동기화 관리
    // ======================
    // 1) Dirty Mark 등록 : 회원 카트 Redis가 수정될 때마다 userId를 “Dirty Set”에 추가
    @Override
    public void markUserCartDirty(Long userId) {
        stringRedisTemplate.opsForSet().add(USER_CART_DIRTY_SET_KEY, String.valueOf(userId));
    }

    @Override
    public void markUserItemDirty(Long userId, Long bookId) {

    }

    @Override
    public void markUserItemDeleted(Long userId, Long bookId) {

    }

    // 6) Dirty ID 목록 조회 : Dirty Set에 들어있는 userId에 대해서만 DB flush가 발생. (Scheduler 사용)
    @Override
    public Set<Long> getDirtyUserIds() {
        Set<String> members = stringRedisTemplate.opsForSet().members(USER_CART_DIRTY_SET_KEY);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getDirtyItemIds(Long userId) {
        return Set.of();
    }

    @Override
    public Set<Long> getDeletedItemIds(Long userId) {
        return Set.of();
    }

    // 7) Dirty Mark 해제 : flush 완료 후, Dirty Set에서 해당 userId 제거
    @Override
    public void clearUserCartDirty(Long userId) {
        stringRedisTemplate.opsForSet().remove(USER_CART_DIRTY_SET_KEY, String.valueOf(userId));
    }

    @Override
    public void markUserCartClear(Long userId) {

    }

    @Override
    public boolean consumeUserCartClear(Long userId) {
        return false;
    }

    @Override
    public void cleanupUserDirtyKeys(Long userId) {

    }

}
