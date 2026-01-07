package com.nhnacademy.book2onandon_order_payment_service.cart.repository;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartRedisItem;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartErrorCode;
import java.util.HashMap;
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

import static com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartConstants.*;

// "Redis 장바구니 저장소”를 직접 다루는(Redis에 어떻게 저장·조회·삭제할지) 구현체
// Spring Data JPA는 자동 구현을 제공하지만 Redis는 자동 Repository 구현을 제공하지 않기 때문에 직접 인터페이스 + 구현체(Impl)를 만든 것.
@RequiredArgsConstructor
@Repository
public class CartRedisRepositoryImpl implements CartRedisRepository {

    // redisTemplate은 Redis 서버 연결을 자동으로 관리한다.
    private final RedisTemplate<String, CartRedisItem> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DIRTY_USERS = USER_CART_DIRTY_SET_KEY;

    // ======================
    // 0. Validate 헬퍼
    // ======================
    private void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CartBusinessException(CartErrorCode.INVALID_USER_ID);
        }
    }

    private void requireUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new CartBusinessException(CartErrorCode.INVALID_GUEST_UUID);
        }
    }

    private void requireBookId(Long bookId) {
        if (bookId == null || bookId <= 0) {
            throw new CartBusinessException(CartErrorCode.INVALID_BOOK_ID);
        }
    }


    // ======================
    // 1. Redis 기본 설정
    // ======================
    // 1) Hash 타입으로 저장된 장바구니 데이터에 접근
    private HashOperations<String, Long, CartRedisItem> hashOps() {
        return redisTemplate.opsForHash();
    }

    // 2) 저장소 키를 결정
    private String userKey(Long userId) {
        return USER_CART_KEY_PREFIX + userId;
    }
    private String guestKey(String uuid) {
        return GUEST_CART_KEY_PREFIX + uuid;
    }

    // 3) 수량(quantity)을 min~max 범위 안으로 강제하는 로직
    private int capQuantity(int quantity) {
        return Math.clamp(quantity, MIN_QUANTITY, MAX_QUANTITY);
    }

    // 4) 비회원 장바구니의 TTL을 갱신하는 기능
    private void extendGuestTtl(String uuid) {
        requireUuid(uuid);

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
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap(); // 비어있는 불변 Map 객체
        }
        return map;
    }

    // 2) 캐시 채워넣기 : DB → Redis 캐시로 복사
    @Override
    public void putUserItem(Long userId, CartRedisItem cartRedisItem) {
        requireUserId(userId);
        if (cartRedisItem == null) {
            throw new CartBusinessException(CartErrorCode.CART_ITEM_NOT_FOUND, "cartRedisItem이 비어있습니다.");
        }
        requireBookId(cartRedisItem.getBookId());

        String key = userKey(userId);
        cartRedisItem.setQuantity(capQuantity(cartRedisItem.getQuantity())); // 수량이 정해진 최대/최소 허용 범위를 벗어나지 않도록

        // 기존 동일 bookId만 조회 (HGET)
        CartRedisItem existing = hashOps().get(key, cartRedisItem.getBookId());
        long now = System.currentTimeMillis();

        if (existing != null) {
            // 기존 createdAt 유지
            if (existing.getCreatedAt() > 0) cartRedisItem.setCreatedAt(existing.getCreatedAt());
            // updatedAt 갱신
            cartRedisItem.setUpdatedAt(now);
        } else {
            // 신규라면 createdAt 초기화
            if (cartRedisItem.getCreatedAt() == 0) cartRedisItem.setCreatedAt(now);
            cartRedisItem.setUpdatedAt(now);
        }

        hashOps().put(key, cartRedisItem.getBookId(), cartRedisItem);
    }

    // 3) 캐시 단일 삭제 : 회원 장바구니에서 특정 상품을 삭제
    @Override
    public void deleteUserCartItem(Long userId, long bookId) {
        requireUserId(userId);
        if (bookId <= 0) throw new CartBusinessException(CartErrorCode.INVALID_BOOK_ID);
        hashOps().delete(userKey(userId), bookId);
    }

    // 4) 캐시 무효화 : Merge, flush 후 Redis 캐시 삭제(리셋)
    @Override
    public void clearUserCart(Long userId) {
        requireUserId(userId);
        redisTemplate.delete(userKey(userId));
    }

    @Override
    public void putUserItems(Long userId, Map<Long, CartRedisItem> items) {
        requireUserId(userId);
        if (items == null || items.isEmpty()) return;

        String key = userKey(userId);

        // 기존 createdAt 유지가 필요하면 기존값을 한 번에 읽는다
        Map<Long, CartRedisItem> existing = hashOps().entries(key);
        if (existing == null) {
            throw new CartBusinessException(
                    CartErrorCode.DIRTY_SET_CORRUPTED,
                    "Redis hash entries returned null. key=" + key
            );
        }
        long now = System.currentTimeMillis();

        for (CartRedisItem it : items.values()) {
            if (it == null) {
                throw new CartBusinessException(CartErrorCode.CART_ITEM_NOT_FOUND, "CartRedisItem 항목에 null 값이 포함되어 있습니다.");
            }
            requireBookId(it.getBookId());

            it.setQuantity(capQuantity(it.getQuantity()));
            CartRedisItem old = existing.get(it.getBookId());
            if (old != null && old.getCreatedAt() > 0) it.setCreatedAt(old.getCreatedAt());
            if (it.getCreatedAt() == 0) it.setCreatedAt(now);
            it.setUpdatedAt(now);
        }

        // 한 번에 HMSET
        hashOps().putAll(key, items);
    }


    // ======================
    // 2. 비회원 장바구니
    // ======================
    // 1) 전체 조회
    @Override
    public Map<Long, CartRedisItem> getGuestCartItems(String uuid) {
        requireUuid(uuid);
        Map<Long, CartRedisItem> map = hashOps().entries(guestKey(uuid));
        return map != null ? map : Collections.emptyMap();
    }

    // 2) 상태 갱신
    @Override
    public void putGuestItem(String uuid, CartRedisItem cartRedisItem) {
        requireUuid(uuid);
        // 단순히 null 체크만 하는 것이 아니라, 내부 데이터가 유효한지 확인
        if (cartRedisItem == null || cartRedisItem.getBookId() == null) {
            throw new CartBusinessException(
                    CartErrorCode.CART_ITEM_NOT_FOUND,
                    "장바구니 아이템 정보가 올바르지 않습니다."
            );
        }
        requireBookId(cartRedisItem.getBookId());

        String key = guestKey(uuid);
        cartRedisItem.setQuantity(capQuantity(cartRedisItem.getQuantity()));

        CartRedisItem existing = hashOps().get(key, cartRedisItem.getBookId());
        long now = System.currentTimeMillis();

        if (existing != null) {
            if (existing.getCreatedAt() > 0) cartRedisItem.setCreatedAt(existing.getCreatedAt());
            cartRedisItem.setUpdatedAt(now);
        } else {
            if (cartRedisItem.getCreatedAt() == 0) cartRedisItem.setCreatedAt(now);
            cartRedisItem.setUpdatedAt(now);
        }

        hashOps().put(key, cartRedisItem.getBookId(), cartRedisItem);
        extendGuestTtl(uuid);
    }

    // 3) 수량 변경
    @Override
    public void updateGuestItemQuantity(String uuid, long bookId, int quantity) {
        requireUuid(uuid);
        if (bookId <= 0) throw new CartBusinessException(CartErrorCode.INVALID_BOOK_ID);

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
        requireUuid(uuid);
        if (bookId <= 0) throw new CartBusinessException(CartErrorCode.INVALID_BOOK_ID);

        hashOps().delete(guestKey(uuid), bookId);
        extendGuestTtl(uuid);
    }

    // 5) 전체 삭제(리셋) (로그인 후 병합 완료, 주문 완료, 세션/UUID 만료 등..)
    @Override
    public void clearGuestCart(String uuid) {
        requireUuid(uuid);
        redisTemplate.delete(guestKey(uuid));
    }

    @Override
    public void putGuestItems(String uuid, Map<Long, CartRedisItem> items) {
        requireUuid(uuid);
        if (items == null || items.isEmpty()) {
            // 비어있는 요청이면 TTL만 연장할지 정책 선택 가능
            extendGuestTtl(uuid);
            return;
        }

        String key = guestKey(uuid);

        // 기존 createdAt 유지 (guest 카트는 보통 “카트 생성 시각”을 item마다 동일하게 유지하려는 의도)
        long now = System.currentTimeMillis();
        long createdAt = now;

        Map<Long, CartRedisItem> existingItems = HashMap.newHashMap(items.size());
        if (existingItems != null && !existingItems.isEmpty()) {
            // 기존 로직과 동일한 방식: 아무 아이템 하나의 createdAt을 카트 createdAt으로 취급
            CartRedisItem any = existingItems.values().iterator().next();
            if (any != null && any.getCreatedAt() > 0) {
                createdAt = any.getCreatedAt();
            }
        }

        // putAll로 밀어넣을 값 준비: cap/createdAt/updatedAt 정리
        Map<Long, CartRedisItem> toSave = new java.util.HashMap<>(items.size());
        for (Map.Entry<Long, CartRedisItem> e : items.entrySet()) {
            Long bookId = e.getKey();
            CartRedisItem v = e.getValue();
            if (bookId == null || bookId <= 0) {
                throw new CartBusinessException(CartErrorCode.INVALID_BOOK_ID);
            }
            if (v == null) {
                throw new CartBusinessException(CartErrorCode.CART_ITEM_NOT_FOUND, "items에 null값이 포함되어 있습니다.");
            }

            int cappedQty = capQuantity(v.getQuantity());
            boolean selected = v.isSelected(); // 또는 기본값 정책 적용 가능

            CartRedisItem normalized = new CartRedisItem(bookId, cappedQty, selected, createdAt, now);
            toSave.put(bookId, normalized);
        }

        if (!toSave.isEmpty()) {
            hashOps().putAll(key, toSave);   // HMSET 1회
        }

        extendGuestTtl(uuid); // TTL 연장 1회
    }


    // ======================
    // 3. 장바구니 동기화 관리
    // ======================
    // 1) Dirty Mark 등록 : 회원 카트 Redis가 수정될 때마다 userId를 “Dirty Set”에 추가
    @Override
    public void markUserCartDirty(Long userId) {
        requireUserId(userId);
        stringRedisTemplate.opsForSet().add(USER_CART_DIRTY_SET_KEY, String.valueOf(userId));
    }

    // 6) Dirty ID 목록 조회 : Dirty Set에 들어있는 userId에 대해서만 DB flush가 발생. (Scheduler 사용)
    @Override
    public Set<Long> getDirtyUserIds() {
        Set<String> users = stringRedisTemplate.opsForSet().members(USER_CART_DIRTY_SET_KEY);
        if (users == null || users.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            return users.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            // Dirty Set이 오염된 상태(문자열이 섞임). 시스템 내부 오류로 처리.
            throw new CartBusinessException(CartErrorCode.DIRTY_SET_CORRUPTED,
                    "Dirty ID에 숫자가 아닌 값이 포함되어 있습니다. users=" + users);
        }
    }

    // 7) Dirty Mark 해제 : flush 완료 후, Dirty Set에서 해당 userId 제거
    @Override
    public void clearUserCartDirty(Long userId) {
        requireUserId(userId);
        stringRedisTemplate.opsForSet().remove(USER_CART_DIRTY_SET_KEY, String.valueOf(userId));
    }

    @Override
    public boolean isUserCartDirty(Long userId) {
        if (userId == null) return false;
        Boolean result = stringRedisTemplate.opsForSet()
                .isMember(DIRTY_USERS, String.valueOf(userId));
        return Boolean.TRUE.equals(result);
    }

}
