package com.nhnacademy.book2onandon_order_payment_service.cart.repository;

import static com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartConstants.GUEST_CART_KEY_PREFIX;
import static com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartConstants.MAX_QUANTITY;
import static com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartConstants.USER_CART_DIRTY_SET_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartRedisItem;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartErrorCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
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
        // @InjectMocks 대신 직접 생성 (주입 불확실성 제거)
        repository = new CartRedisRepositoryImpl(redisTemplate, stringRedisTemplate); // CartRedisRepositoryImpl repository;에 아무 어노테이션 없을 경우에 필요함.

        // 어떤 테스트에서는 안 쓰일 수 있으니 lenient 처리
        lenient().doReturn(hashOps).when(redisTemplate).opsForHash();
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOps);
    }

    @Nested
    @DisplayName("Dirty Set")
    class DirtySet {
        @Test
        void isUserCartDirty_userIdNull_false() {
            boolean result = repository.isUserCartDirty(null);
            assertThat(result).isFalse();
            // opsForSet이 호출되지 않아도 lenient라서 불필요 스텁 예외 안 남
            verifyNoInteractions(setOps);
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
    }

    @Nested
    @DisplayName("updateGuestItemQuantity")
    class UpdateGuestQty {
        @Test
        void capAndPut_newItem() {
            String uuid = "guest-1";
            String key = GUEST_CART_KEY_PREFIX + uuid;

            when(hashOps.entries(key)).thenReturn(Collections.emptyMap()); // createdAt 산정용
            when(hashOps.get(key, 10L)).thenReturn(null);

            repository.updateGuestItemQuantity(uuid, 10L, 9999);

            ArgumentCaptor<CartRedisItem> captor = ArgumentCaptor.forClass(CartRedisItem.class);
            verify(hashOps).put(eq(key), eq(10L), captor.capture());

            CartRedisItem saved = captor.getValue();
            assertThat(saved.getQuantity()).isEqualTo(MAX_QUANTITY);
            assertThat(saved.getCreatedAt()).isGreaterThan(0L);
            assertThat(saved.getUpdatedAt()).isGreaterThan(0L);

            // TTL은 여기서 검증하지 않음 (extendGuestTtl에서 early return 가능)
            verify(redisTemplate, never()).expire(anyString(), any());
        }
    }
}
