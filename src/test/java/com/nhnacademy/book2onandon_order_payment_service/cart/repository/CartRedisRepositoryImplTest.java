package com.nhnacademy.book2onandon_order_payment_service.cart.repository;

import static com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartRedisItem;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartErrorCode;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class CartRedisRepositoryImplTest {

    @Mock
    RedisTemplate<String, CartRedisItem> redisTemplate;

    @Mock
    StringRedisTemplate stringRedisTemplate;

    @Mock
    HashOperations<String, Long, CartRedisItem> hashOps;

    @Mock
    SetOperations<String, String> setOps;

    CartRedisRepositoryImpl repository;

    @BeforeEach
    void setup() {
        repository = new CartRedisRepositoryImpl(redisTemplate, stringRedisTemplate);

        // opsForHash는 doReturn이 안정적
        lenient().doReturn(hashOps).when(redisTemplate).opsForHash();
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOps);
    }

    // =========================
    // Helpers
    // =========================
    private static CartRedisItem item(long bookId, int qty, boolean selected, long createdAt, long updatedAt) {
        return new CartRedisItem(bookId, qty, selected, createdAt, updatedAt);
    }

    private static String userKey(long userId) {
        return USER_CART_KEY_PREFIX + userId;
    }

    private static String guestKey(String uuid) {
        return GUEST_CART_KEY_PREFIX + uuid;
    }

    // =========================================================
    // 1) Dirty Set
    // =========================================================
    @Nested
    @DisplayName("Dirty Set")
    class DirtySet {

        @Test
        void isUserCartDirty_userIdNull_false() {
            boolean result = repository.isUserCartDirty(null);
            assertThat(result).isFalse();
            verify(setOps, never()).isMember(anyString(), anyString());
        }

        @Test
        void markUserCartDirty_invalidUserId_throws() {
            assertThatThrownBy(() -> repository.markUserCartDirty(0L))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_USER_ID));
        }

        @Test
        void markUserCartDirty_ok() {
            repository.markUserCartDirty(1L);
            verify(setOps).add(USER_CART_DIRTY_SET_KEY, "1");
        }

        @Test
        void getDirtyUserIds_empty_returnsEmpty() {
            when(setOps.members(USER_CART_DIRTY_SET_KEY)).thenReturn(Collections.emptySet());

            Set<Long> result = repository.getDirtyUserIds();

            assertThat(result)
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        void getDirtyUserIds_ok() {
            when(setOps.members(USER_CART_DIRTY_SET_KEY))
                    .thenReturn(new HashSet<>(Arrays.asList("1", "2", "3")));

            Set<Long> result = repository.getDirtyUserIds();

            Assertions.assertEquals(Set.of(1L, 2L, 3L), result);
        }

        @Test
        void getDirtyUserIds_corrupted_throws() {
            when(setOps.members(USER_CART_DIRTY_SET_KEY))
                    .thenReturn(new HashSet<>(Arrays.asList("1", "abc")));

            assertThatThrownBy(() -> repository.getDirtyUserIds())
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.DIRTY_SET_CORRUPTED));
        }

        @Test
        void clearUserCartDirty_ok() {
            repository.clearUserCartDirty(10L);
            verify(setOps).remove(USER_CART_DIRTY_SET_KEY, "10");
        }

        @Test
        void isUserCartDirty_trueFalse() {
            when(setOps.isMember(USER_CART_DIRTY_SET_KEY, "1")).thenReturn(Boolean.TRUE);
            when(setOps.isMember(USER_CART_DIRTY_SET_KEY, "2")).thenReturn(Boolean.FALSE);

            assertThat(repository.isUserCartDirty(1L)).isTrue();
            assertThat(repository.isUserCartDirty(2L)).isFalse();
        }
    }

    // =========================================================
    // 2) updateGuestItemQuantity
    // =========================================================
    @Nested
    @DisplayName("updateGuestItemQuantity")
    class UpdateGuestQty {

        @Test
        @DisplayName("신규: cap 적용 + put + extendGuestTtl(entries empty -> expire 없음)")
        void capAndPut_newItem() {
            String uuid = "guest-1";
            String key = guestKey(uuid);

            // updateGuestItemQuantity 내부에서 entries(key) 1회( createdAt 산정 )
            // extendGuestTtl 내부에서 entries(key) 1회
            when(hashOps.entries(key)).thenReturn(Collections.emptyMap(), Collections.emptyMap());
            when(hashOps.get(key, 10L)).thenReturn(null);

            repository.updateGuestItemQuantity(uuid, 10L, 9999);

            ArgumentCaptor<CartRedisItem> captor = ArgumentCaptor.forClass(CartRedisItem.class);
            verify(hashOps).put(eq(key), eq(10L), captor.capture());

            CartRedisItem saved = captor.getValue();
            assertThat(saved.getQuantity()).isEqualTo(MAX_QUANTITY);
            assertThat(saved.getCreatedAt()).isPositive();
            assertThat(saved.getUpdatedAt()).isPositive();

            verify(redisTemplate, never()).expire(anyString(), any());
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("기존: quantity/updatedAt 갱신 + TTL expire (createdAt은 최근값 필요)")
        void existing_updates_and_expire() {
            String uuid = "g5";
            String key = guestKey(uuid);

            long createdAtOk = System.currentTimeMillis(); // 오래된 값 넣으면 delete 분기
            CartRedisItem current = item(10L, 1, true, createdAtOk, 222L);

            when(hashOps.get(key, 10L)).thenReturn(current);
            when(hashOps.entries(key)).thenReturn(Map.of(10L, current), Map.of(10L, current));

            repository.updateGuestItemQuantity(uuid, 10L, 2);

            ArgumentCaptor<CartRedisItem> captor = ArgumentCaptor.forClass(CartRedisItem.class);
            verify(hashOps).put(eq(key), eq(10L), captor.capture());

            CartRedisItem saved = captor.getValue();
            assertThat(saved.getQuantity()).isEqualTo(2);
            assertThat(saved.getCreatedAt()).isEqualTo(createdAtOk);
            assertThat(saved.getUpdatedAt()).isPositive();

            verify(redisTemplate).expire(key, Duration.ofHours(GUEST_CART_TTL_HOURS));
            verify(redisTemplate, never()).delete(key);
        }
    }

    // =========================================================
    // 3) 회원 장바구니
    // =========================================================
    @Nested
    @DisplayName("User Cart")
    class UserCart {

        @Test
        @DisplayName("getUserCartItems: redis empty면 emptyMap 반환")
        void getUserCartItems_empty_returnsEmptyMap() {
            when(hashOps.entries(userKey(1L))).thenReturn(Collections.emptyMap());

            Map<Long, CartRedisItem> result = repository.getUserCartItems(1L);

            assertThat(result)
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @DisplayName("getUserCartItems: redis에 값 있으면 그대로 반환")
        void getUserCartItems_nonEmpty_returnsMap() {
            Map<Long, CartRedisItem> stored = new HashMap<>();
            stored.put(10L, item(10L, 2, true, 100L, 200L));
            when(hashOps.entries(userKey(1L))).thenReturn(stored);

            Map<Long, CartRedisItem> result = repository.getUserCartItems(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(10L).getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("putUserItem: userId invalid면 예외")
        void putUserItem_invalidUserId_throws() {
            CartRedisItem it = item(10L, 1, true, 0, 0);

            assertThatThrownBy(() -> repository.putUserItem(0L, it))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_USER_ID));

            verify(hashOps, never()).put(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("putUserItem: cartRedisItem null이면 예외")
        void putUserItem_nullItem_throws() {
            assertThatThrownBy(() -> repository.putUserItem(1L, null))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.CART_ITEM_NOT_FOUND));

            verify(hashOps, never()).put(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("putUserItem: bookId invalid면 예외")
        void putUserItem_invalidBookId_throws() {
            CartRedisItem it = item(0L, 1, true, 0, 0);

            assertThatThrownBy(() -> repository.putUserItem(1L, it))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_BOOK_ID));

            verify(hashOps, never()).put(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("putUserItem: existing 없으면 createdAt 신규 세팅, updatedAt 세팅, quantity cap")
        void putUserItem_newItem_setsCreatedAtAndCapsQty() {
            long userId = 1L;
            String key = userKey(userId);

            CartRedisItem input = item(10L, 99999, true, 0, 0);
            when(hashOps.get(key, 10L)).thenReturn(null);

            repository.putUserItem(userId, input);

            ArgumentCaptor<CartRedisItem> captor = ArgumentCaptor.forClass(CartRedisItem.class);
            verify(hashOps).put(eq(key), eq(10L), captor.capture());

            CartRedisItem saved = captor.getValue();
            assertThat(saved.getQuantity()).isEqualTo(MAX_QUANTITY);
            assertThat(saved.getCreatedAt()).isPositive();
            assertThat(saved.getUpdatedAt()).isPositive();
        }

        @Test
        @DisplayName("putUserItem: existing 있으면 createdAt 유지, updatedAt 갱신")
        void putUserItem_existing_preservesCreatedAt() {
            long userId = 1L;
            String key = userKey(userId);

            CartRedisItem existing = item(10L, 2, true, 1234L, 2000L);
            when(hashOps.get(key, 10L)).thenReturn(existing);

            CartRedisItem input = item(10L, 3, true, 0, 0);

            repository.putUserItem(userId, input);

            ArgumentCaptor<CartRedisItem> captor = ArgumentCaptor.forClass(CartRedisItem.class);
            verify(hashOps).put(eq(key), eq(10L), captor.capture());

            CartRedisItem saved = captor.getValue();
            assertThat(saved.getCreatedAt()).isEqualTo(1234L);
            assertThat(saved.getUpdatedAt()).isPositive();
        }

        @Test
        @DisplayName("deleteUserCartItem: bookId invalid면 예외")
        void deleteUserCartItem_invalidBookId_throws() {
            assertThatThrownBy(() -> repository.deleteUserCartItem(1L, 0L))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_BOOK_ID));
        }

        @Test
        @DisplayName("deleteUserCartItem: 정상 삭제")
        void deleteUserCartItem_ok() {
            repository.deleteUserCartItem(1L, 10L);
            verify(hashOps).delete(userKey(1L), 10L);
        }

        @Test
        @DisplayName("clearUserCart: 정상 삭제")
        void clearUserCart_ok() {
            repository.clearUserCart(1L);
            verify(redisTemplate).delete(userKey(1L));
        }

        @Test
        @DisplayName("putUserItems: items null/empty면 아무것도 안함")
        void putUserItems_nullOrEmpty_noop() {
            repository.putUserItems(1L, null);
            repository.putUserItems(1L, Collections.emptyMap());

            verify(hashOps, never()).putAll(anyString(), anyMap());
        }

        @Test
        @DisplayName("putUserItems: entries가 null이면 DIRTY_SET_CORRUPTED 예외")
        void putUserItems_entriesNull_throws() {
            String key = userKey(1L);
            when(hashOps.entries(key)).thenReturn(null);

            Map<Long, CartRedisItem> items = Map.of(10L, item(10L, 1, true, 0, 0));

            assertThatThrownBy(() -> repository.putUserItems(1L, items))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.DIRTY_SET_CORRUPTED));

            verify(hashOps, never()).putAll(anyString(), anyMap());
        }

        @Test
        @DisplayName("putUserItems: null item 포함이면 예외")
        void putUserItems_containsNullItem_throws() {
            String key = userKey(1L);
            when(hashOps.entries(key)).thenReturn(Collections.emptyMap());

            Map<Long, CartRedisItem> items = new HashMap<>();
            items.put(10L, null);

            assertThatThrownBy(() -> repository.putUserItems(1L, items))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.CART_ITEM_NOT_FOUND));
        }

        @Test
        @DisplayName("putUserItems: 정상 - cap, createdAt 유지/세팅, updatedAt 세팅 후 putAll")
        void putUserItems_ok_capsAndTimestamps() {
            long userId = 1L;
            String key = userKey(userId);

            Map<Long, CartRedisItem> existing = Map.of(10L, item(10L, 2, true, 555L, 777L));
            when(hashOps.entries(key)).thenReturn(existing);

            Map<Long, CartRedisItem> input = new HashMap<>();
            input.put(10L, item(10L, 99999, true, 0, 0));
            input.put(11L, item(11L, 0, true, 0, 0));

            repository.putUserItems(userId, input);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<Long, CartRedisItem>> captor = ArgumentCaptor.forClass(Map.class);
            verify(hashOps).putAll(eq(key), captor.capture());

            Map<Long, CartRedisItem> saved = captor.getValue();
            assertThat(saved.get(10L).getQuantity()).isEqualTo(MAX_QUANTITY);
            assertThat(saved.get(10L).getCreatedAt()).isEqualTo(555L);
            assertThat(saved.get(10L).getUpdatedAt()).isPositive();

            assertThat(saved.get(11L).getCreatedAt()).isPositive();
            assertThat(saved.get(11L).getUpdatedAt()).isPositive();
        }
    }

    // =========================================================
    // 4) 비회원 장바구니
    // =========================================================
    @Nested
    @DisplayName("Guest Cart")
    class GuestCart {

        @Test
        @DisplayName("getGuestCartItems: uuid blank면 예외")
        void getGuestCartItems_invalidUuid_throws() {
            assertThatThrownBy(() -> repository.getGuestCartItems("  "))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_GUEST_UUID));
        }

        @Test
        @DisplayName("getGuestCartItems: entries null이면 null 반환(현재 구현 기준)")
        void getGuestCartItems_entriesNull_returnsNull() {
            String uuid = "g1";
            when(hashOps.entries(guestKey(uuid))).thenReturn(null);

            Map<Long, CartRedisItem> result = repository.getGuestCartItems(uuid);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("putGuestItem: item null 또는 bookId invalid면 예외")
        void putGuestItem_invalidItem_throws() {
            // case 1) item == null
            Throwable t1 = catchThrowable(() -> repository.putGuestItem("g1", null));

            assertThat(t1)
                    .isInstanceOf(CartBusinessException.class);
            assertThat(((CartBusinessException) t1).getErrorCode())
                    .isEqualTo(CartErrorCode.CART_ITEM_NOT_FOUND);

            // case 2) bookId invalid
            Throwable t2 = catchThrowable(() -> repository.putGuestItem("g1", item(0L, 1, true, 0, 0)));

            assertThat(t2)
                    .isInstanceOf(CartBusinessException.class);
            assertThat(((CartBusinessException) t2).getErrorCode())
                    .isEqualTo(CartErrorCode.INVALID_BOOK_ID);
        }

        @Test
        @DisplayName("putGuestItem: extendGuestTtl - entries empty면 expire 호출 안함(early return)")
        void putGuestItem_extendTtl_entriesEmpty_noExpire() {
            String uuid = "g1";
            String key = guestKey(uuid);

            when(hashOps.get(key, 10L)).thenReturn(null);
            when(hashOps.entries(key)).thenReturn(Collections.emptyMap()); // extendGuestTtl early return

            repository.putGuestItem(uuid, item(10L, 5, true, 0, 0));

            verify(hashOps).put(eq(key), eq(10L), any(CartRedisItem.class));
            verify(redisTemplate, never()).expire(anyString(), any());
            verify(redisTemplate, never()).delete(key);
        }

        @Test
        @DisplayName("putGuestItem: extendGuestTtl - maxLifetime 초과면 delete 호출")
        void putGuestItem_extendTtl_expiredByMaxLifetime_deletes() {
            String uuid = "g2";
            String key = guestKey(uuid);

            when(hashOps.get(key, 10L)).thenReturn(null);

            long now = System.currentTimeMillis();
            long maxLifetimeMillis = GUEST_CART_MAX_LIFETIME_DAYS * 24L * 60L * 60L * 1000L;
            long tooOldCreatedAt = now - (maxLifetimeMillis + 10_000);

            Map<Long, CartRedisItem> existing = Map.of(10L, item(10L, 1, true, tooOldCreatedAt, tooOldCreatedAt));
            when(hashOps.entries(key)).thenReturn(existing);

            repository.putGuestItem(uuid, item(10L, 1, true, 0, 0));

            verify(redisTemplate).delete(key);
            verify(redisTemplate, never()).expire(eq(key), any());
        }

        @Test
        @DisplayName("putGuestItem: extendGuestTtl - 정상 범위면 expire 호출")
        void putGuestItem_extendTtl_ok_expires() {
            String uuid = "g3";
            String key = guestKey(uuid);

            when(hashOps.get(key, 10L)).thenReturn(null);

            long now = System.currentTimeMillis();
            long maxLifetimeMillis = GUEST_CART_MAX_LIFETIME_DAYS * 24L * 60L * 60L * 1000L;
            long okCreatedAt = now - (maxLifetimeMillis / 2);

            Map<Long, CartRedisItem> existing = Map.of(10L, item(10L, 1, true, okCreatedAt, okCreatedAt));
            when(hashOps.entries(key)).thenReturn(existing);

            repository.putGuestItem(uuid, item(10L, 1, true, 0, 0));

            verify(redisTemplate).expire(key, Duration.ofHours(GUEST_CART_TTL_HOURS));
            verify(redisTemplate, never()).delete(key);
        }

        @Test
        @DisplayName("updateGuestItemQuantity: uuid invalid/bookId invalid 예외")
        void updateGuestItemQuantity_invalidArgs_throw() {
            assertThatThrownBy(() -> repository.updateGuestItemQuantity(" ", 10L, 1))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_GUEST_UUID));

            assertThatThrownBy(() -> repository.updateGuestItemQuantity("g1", 0L, 1))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_BOOK_ID));
        }

        @Test
        @DisplayName("deleteGuestItem: bookId invalid 예외")
        void deleteGuestItem_invalidBookId_throws() {
            assertThatThrownBy(() -> repository.deleteGuestItem("g1", 0L))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_BOOK_ID));
        }

        @Test
        @DisplayName("deleteGuestItem: 정상 delete + extendGuestTtl early return")
        void deleteGuestItem_ok() {
            String uuid = "g6";
            String key = guestKey(uuid);

            when(hashOps.entries(key)).thenReturn(Collections.emptyMap()); // extendGuestTtl early return

            repository.deleteGuestItem(uuid, 10L);

            verify(hashOps).delete(key, 10L);
            verify(redisTemplate, never()).expire(anyString(), any());
        }

        @Test
        @DisplayName("clearGuestCart: 정상 delete")
        void clearGuestCart_ok() {
            repository.clearGuestCart("g7");
            verify(redisTemplate).delete(guestKey("g7"));
        }

        @Test
        @DisplayName("putGuestItems: empty면 TTL만 연장 시도(extendGuestTtl early return)")
        void putGuestItems_empty_callsExtendTtl() {
            String uuid = "g8";
            String key = guestKey(uuid);

            when(hashOps.entries(key)).thenReturn(Collections.emptyMap());

            repository.putGuestItems(uuid, Collections.emptyMap());

            verify(hashOps, never()).putAll(anyString(), anyMap());
            verify(redisTemplate, never()).expire(anyString(), any());
        }

        @Test
        @DisplayName("putGuestItems: bookId invalid 또는 value null이면 예외")
        void putGuestItems_invalidInput_throws() {
            String uuid = "g9";

            Map<Long, CartRedisItem> bad1 = new HashMap<>();
            bad1.put(0L, item(0L, 1, true, 0, 0));

            assertThatThrownBy(() -> repository.putGuestItems(uuid, bad1))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.INVALID_BOOK_ID));

            Map<Long, CartRedisItem> bad2 = new HashMap<>();
            bad2.put(10L, null);

            assertThatThrownBy(() -> repository.putGuestItems(uuid, bad2))
                    .isInstanceOf(CartBusinessException.class)
                    .satisfies(ex -> assertThat(((CartBusinessException) ex).getErrorCode())
                            .isEqualTo(CartErrorCode.CART_ITEM_NOT_FOUND));
        }

        @Test
        @DisplayName("putGuestItems: 정상 putAll + TTL expire (현재 구현은 createdAt 항상 now로 세팅됨)")
        void putGuestItems_ok_putAll_and_expire() {
            String uuid = "g10";
            String key = guestKey(uuid);

            Map<Long, CartRedisItem> input = new HashMap<>();
            input.put(10L, item(10L, 99999, true, 0, 0));
            input.put(11L, item(11L, 2, false, 0, 0));

            // extendGuestTtl에서 entries가 비어있지 않게 -> expire 분기
            long createdAtOk = System.currentTimeMillis();
            Map<Long, CartRedisItem> entriesAfter =
                    Map.of(10L, item(10L, 1, true, createdAtOk, 0));
            when(hashOps.entries(key)).thenReturn(entriesAfter);

            repository.putGuestItems(uuid, input);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<Long, CartRedisItem>> captor = ArgumentCaptor.forClass(Map.class);
            verify(hashOps).putAll(eq(key), captor.capture());

            Map<Long, CartRedisItem> saved = captor.getValue();
            assertThat(saved).hasSize(2);

            // createdAt/updatedAt은 now 기반이라 "0보다 큼" 정도만 검증
            assertThat(saved.get(10L).getQuantity()).isEqualTo(MAX_QUANTITY);
            assertThat(saved.get(10L).getCreatedAt()).isPositive();
            assertThat(saved.get(10L).getUpdatedAt()).isPositive();

            verify(redisTemplate).expire(key, Duration.ofHours(GUEST_CART_TTL_HOURS));
        }
    }
}
