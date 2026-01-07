package com.nhnacademy.book2onandon_order_payment_service.cart.service;

import static com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.*;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.*;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.*;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.*;
import com.nhnacademy.book2onandon_order_payment_service.cart.repository.*;
import com.nhnacademy.book2onandon_order_payment_service.cart.support.CartCalculator;
import com.nhnacademy.book2onandon_order_payment_service.client.BookServiceClient;
import com.nhnacademy.book2onandon_order_payment_service.client.BookServiceClient.BookSnapshot;
import feign.FeignException;
import java.util.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private CartRedisRepository cartRedisRepository;
    @Mock private BookServiceClient bookServiceClient;
    @Mock private CartCalculator cartCalculator;

    @InjectMocks private CartServiceImpl cartService;

    // ============================================================
    // Helpers
    // ============================================================

    private static CartRedisItem redisItem(long bookId, int qty, boolean selected) {
        return new CartRedisItem(bookId, qty, selected, 0L, 0L);
    }

    private static CartItem dbItem(Cart cart, long bookId, int qty, boolean selected) {
        CartItem it = CartItem.builder()
                .cart(cart)
                .bookId(bookId)
                .quantity(qty)
                .selected(selected)
                .build();
        return it;
    }

    private static BookSnapshot snapshot(long bookId, int stock, boolean deleted, boolean saleEnded) {
        BookSnapshot s = new BookSnapshot();
        s.setBookId(bookId);
        s.setTitle("title-" + bookId);
        s.setThumbnailUrl("thumb-" + bookId);
        s.setOriginalPrice(2000);
        s.setSalePrice(1500);
        s.setStockCount(stock);
        s.setDeleted(deleted);
        s.setSaleEnded(saleEnded);
        return s;
    }

    private void stubSnapshots(List<Long> bookIds, Map<Long, BookSnapshot> map) {
        // requireBookSnapshot/safeGetBookSnapshots 모두 getBookSnapshots(List<Long>)를 호출
        given(bookServiceClient.getBookSnapshots(anyList())).willAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) inv.getArgument(0);
            // 테스트 안전성: 요청한 ID 중 없는 것은 null로 남게 됨
            Map<Long, BookSnapshot> res = new HashMap<>();
            for (Long id : ids) {
                if (map.containsKey(id)) res.put(id, map.get(id));
            }
            return res;
        });
    }

    private void stubPricingAnyMap(long bookId, int qty,
                                   boolean available,
                                   CartItemUnavailableReason reason,
                                   int stock) {

        given(cartCalculator.calculatePricing(eq(bookId), eq(qty), anyMap()))
                .willReturn(new CartCalculator.CartItemPricingResult(
                        "t", "u",
                        2000, 1500,
                        stock,
                        false,
                        available,
                        reason,
                        1500 * qty
                ));
    }

    // ============================================================
    // Quantity validation (via public methods)
    // ============================================================
    @Nested
    class QuantityValidation {

        @Test
        void addItemToUserCart_whenQuantityUnderMin_throwInvalidQuantity() {
            long userId = 1L;
            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(10L);
            given(req.getQuantity()).willReturn(MIN_QUANTITY - 1);
            given(req.isSelected()).willReturn(true);

            assertThatThrownBy(() -> cartService.addItemToUserCart(userId, req))
                    .isInstanceOf(CartBusinessException.class);

            verifyNoInteractions(cartRedisRepository, bookServiceClient);
        }

        @Test
        void addItemToGuestCart_whenQuantityOverMax_throwInvalidQuantity() {
            String uuid = "g1";
            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(10L);
            given(req.getQuantity()).willReturn(MAX_QUANTITY + 1);
            given(req.isSelected()).willReturn(true);

            assertThatThrownBy(() -> cartService.addItemToGuestCart(uuid, req))
                    .isInstanceOf(CartBusinessException.class);

            verifyNoInteractions(cartRedisRepository, bookServiceClient);
        }
    }

    // ============================================================
    // requireBookSnapshot / availability / stock (via add/update)
    // ============================================================
    @Nested
    class BookSnapshotAndStock {

        @Test
        void addItemToUserCart_whenSnapshotMissing_throwInvalidBookId() {
            long userId = 1L;
            long bookId = 10L;

            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(1);
            given(req.isSelected()).willReturn(true);

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());
            // getBookSnapshots returns empty => snapshot null
            given(bookServiceClient.getBookSnapshots(anyList())).willReturn(Collections.emptyMap());

            assertThatThrownBy(() -> cartService.addItemToUserCart(userId, req))
                    .isInstanceOf(CartBusinessException.class);

            verify(cartRedisRepository, never()).putUserItem(anyLong(), any());
        }

        @Test
        void addItemToUserCart_whenBookServiceFeignException_throwBookUnavailable() {
            long userId = 1L;
            long bookId = 10L;

            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(1);
            given(req.isSelected()).willReturn(true);

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());

            FeignException fe = mock(FeignException.class);
            given(bookServiceClient.getBookSnapshots(anyList())).willThrow(fe);

            assertThatThrownBy(() -> cartService.addItemToUserCart(userId, req))
                    .isInstanceOf(CartBusinessException.class);

            verify(cartRedisRepository, never()).putUserItem(anyLong(), any());
        }

        @Test
        void addItemToUserCart_whenDeletedBook_throwBookUnavailable() {
            long userId = 1L;
            long bookId = 10L;

            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(1);
            given(req.isSelected()).willReturn(true);

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());
            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 10, true, false)));

            assertThatThrownBy(() -> cartService.addItemToUserCart(userId, req))
                    .isInstanceOf(CartBusinessException.class);

            verify(cartRedisRepository, never()).putUserItem(anyLong(), any());
        }

        @Test
        void addItemToUserCart_whenStockZero_throwOutOfStock() {
            long userId = 1L;
            long bookId = 10L;

            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(1);
            given(req.isSelected()).willReturn(true);

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());
            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 0, false, false)));

            assertThatThrownBy(() -> cartService.addItemToUserCart(userId, req))
                    .isInstanceOf(CartBusinessException.class);

            verify(cartRedisRepository, never()).putUserItem(anyLong(), any());
        }

        @Test
        void updateQuantityGuestCartItem_whenRequestedExceedsStock_throwOutOfStock() {
            String uuid = "g1";
            long bookId = 10L;

            CartItemQuantityUpdateRequestDto req = mock(CartItemQuantityUpdateRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(5);

            given(cartRedisRepository.getGuestCartItems(uuid))
                    .willReturn(Map.of(bookId, redisItem(bookId, 1, true)));

            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 3, false, false)));

            assertThatThrownBy(() -> cartService.updateQuantityGuestCartItem(uuid, req))
                    .isInstanceOf(CartBusinessException.class);

            verify(cartRedisRepository, never()).putGuestItem(anyString(), any());
        }
    }

    // ============================================================
    // Guest cart core flows
    // ============================================================
    @Nested
    class GuestCart {

        @Test
        void getGuestCart_whenNullRedis_returnEmptyDto() {
            String uuid = "g1";
            given(cartRedisRepository.getGuestCartItems(uuid)).willReturn(null);

            CartItemsResponseDto res = cartService.getGuestCart(uuid);

            assertThat(res.getItems()).isEmpty();
            assertThat(res.getTotalItemCount()).isZero();
            verifyNoInteractions(bookServiceClient, cartCalculator);
        }

        @Test
        void addItemToGuestCart_whenNewItemAndExceedMaxSize_throwCartSizeExceeded() {
            String uuid = "g1";
            long bookId = 999L;

            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(1);
            given(req.isSelected()).willReturn(true);

            Map<Long, CartRedisItem> existing = new HashMap<>();
            for (int i = 0; i < MAX_CART_SIZE; i++) {
                existing.put((long) (1000 + i), redisItem(1000 + i, 1, true));
            }
            given(cartRedisRepository.getGuestCartItems(uuid)).willReturn(existing);

            assertThatThrownBy(() -> cartService.addItemToGuestCart(uuid, req))
                    .isInstanceOf(CartBusinessException.class);

            verifyNoInteractions(bookServiceClient);
            verify(cartRedisRepository, never()).putGuestItem(anyString(), any());
        }

        @Test
        void addItemToGuestCart_whenExistingItem_quantityGetsCapped_andSelectedUpdated() {
            String uuid = "g1";
            long bookId = 10L;

            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(MAX_QUANTITY); // 기존 50 + 99 => cap 99
            given(req.isSelected()).willReturn(false);

            CartRedisItem existing = redisItem(bookId, 50, true);
            given(cartRedisRepository.getGuestCartItems(uuid)).willReturn(new HashMap<>(Map.of(bookId, existing)));

            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 999, false, false)));

            cartService.addItemToGuestCart(uuid, req);

            verify(cartRedisRepository).putGuestItem(eq(uuid), argThat(it ->
                    it != null
                            && it.getBookId().equals(bookId)
                            && it.getQuantity() == MAX_QUANTITY
                            && !it.isSelected()
            ));
        }

        @Test
        void selectAllGuestCartItems_whenEmpty_return() {
            String uuid = "g1";
            CartItemSelectAllRequestDto req = mock(CartItemSelectAllRequestDto.class);
            given(req.isSelected()).willReturn(true);

            given(cartRedisRepository.getGuestCartItems(uuid)).willReturn(Collections.emptyMap());

            cartService.selectAllGuestCartItems(uuid, req);

            verify(cartRedisRepository, never()).putGuestItem(anyString(), any());
        }

        @Test
        void deleteSelectedGuestCartItems_whenHasSelected_deleteOnlySelected() {
            String uuid = "g1";

            Map<Long, CartRedisItem> map = new HashMap<>();
            map.put(10L, redisItem(10L, 1, true));
            map.put(11L, redisItem(11L, 1, false));
            map.put(12L, redisItem(12L, 1, true));
            given(cartRedisRepository.getGuestCartItems(uuid)).willReturn(map);

            cartService.deleteSelectedGuestCartItems(uuid);

            verify(cartRedisRepository).deleteGuestItem(uuid, 10L);
            verify(cartRedisRepository).deleteGuestItem(uuid, 12L);
            verify(cartRedisRepository, never()).deleteGuestItem(uuid, 11L);
        }

        @Test
        void getGuestCartCount_whenEmpty_returnZeros() {
            String uuid = "g1";
            given(cartRedisRepository.getGuestCartItems(uuid)).willReturn(Collections.emptyMap());

            CartItemCountResponseDto res = cartService.getGuestCartCount(uuid);

            assertThat(res.getItemCount()).isZero();
            assertThat(res.getTotalQuantity()).isZero();
        }
    }

    // ============================================================
    // User cart core flows
    // ============================================================
    @Nested
    class UserCart {

        @Test
        void addItemToUserCart_whenNewItemAndExceedMaxSize_throwCartSizeExceeded() {
            long userId = 1L;
            long bookId = 999L;

            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(1);
            given(req.isSelected()).willReturn(true);

            Map<Long, CartRedisItem> existing = new HashMap<>();
            for (int i = 0; i < MAX_CART_SIZE; i++) {
                existing.put((long) (1000 + i), redisItem(1000 + i, 1, true));
            }
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(existing);

            assertThatThrownBy(() -> cartService.addItemToUserCart(userId, req))
                    .isInstanceOf(CartBusinessException.class);

            verifyNoInteractions(bookServiceClient);
            verify(cartRedisRepository, never()).putUserItem(anyLong(), any());
            verify(cartRedisRepository, never()).markUserCartDirty(anyLong());
        }

        @Test
        void addItemToUserCart_whenExistingItem_totalCapped_andDirtyMarked() {
            long userId = 1L;
            long bookId = 10L;

            CartItemRequestDto req = mock(CartItemRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(60);
            given(req.isSelected()).willReturn(false);

            CartRedisItem existing = redisItem(bookId, 50, true); // 50 + 60 => cap 99
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(new HashMap<>(Map.of(bookId, existing)));

            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 999, false, false)));

            cartService.addItemToUserCart(userId, req);

            verify(cartRedisRepository).putUserItem(eq(userId), argThat(it ->
                    it != null
                            && it.getBookId().equals(bookId)
                            && it.getQuantity() == MAX_QUANTITY
                            && !it.isSelected()
            ));
            verify(cartRedisRepository).markUserCartDirty(userId);
        }

        @Test
        void updateQuantityUserCartItem_whenItemMissing_throwNotFound() {
            long userId = 1L;
            long bookId = 10L;

            CartItemQuantityUpdateRequestDto req = mock(CartItemQuantityUpdateRequestDto.class);
            given(req.getBookId()).willReturn(bookId);
            given(req.getQuantity()).willReturn(2);

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());

            assertThatThrownBy(() -> cartService.updateQuantityUserCartItem(userId, req))
                    .isInstanceOf(CartItemNotFoundException.class);

            verifyNoInteractions(bookServiceClient);
        }

        @Test
        void selectAllUserCartItems_whenNothingChanges_doNotMarkDirty() {
            long userId = 1L;

            CartItemSelectAllRequestDto req = mock(CartItemSelectAllRequestDto.class);
            given(req.isSelected()).willReturn(true);

            Map<Long, CartRedisItem> map = new HashMap<>();
            map.put(10L, redisItem(10L, 1, true));
            map.put(11L, redisItem(11L, 1, true));
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(map);

            cartService.selectAllUserCartItems(userId, req);

            verify(cartRedisRepository, never()).markUserCartDirty(userId);
        }

        @Test
        void deleteSelectedUserCartItems_whenEmpty_return() {
            long userId = 1L;
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());

            cartService.deleteSelectedUserCartItems(userId);

            verify(cartRedisRepository, never()).deleteUserCartItem(anyLong(), anyLong());
            verify(cartRedisRepository, never()).markUserCartDirty(userId);
        }

        @Test
        void clearUserCart_deleteRedisAndDb_andClearDirty() {
            long userId = 1L;
            Cart cart = Cart.builder().userId(userId).build();
            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

            cartService.clearUserCart(userId);

            verify(cartRedisRepository).clearUserCart(userId);
            verify(cartItemRepository).deleteByCart(cart);
            verify(cartRedisRepository).clearUserCartDirty(userId);
        }
    }

    // ============================================================
    // getUserCart + builders (Redis hit / dirty block / DB warm-up)
    // ============================================================
    @Nested
    class GetUserCart {

        @Test
        void redisHit_buildFromRedis_pricingAndTotalsCovered() {
            long userId = 1L;
            long bookId1 = 10L;
            long bookId2 = 11L;

            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(
                            bookId1, redisItem(bookId1, 2, true),
                            bookId2, redisItem(bookId2, 1, false)
                    ));

            stubSnapshots(List.of(bookId1, bookId2), Map.of(
                    bookId1, snapshot(bookId1, 100, false, false),
                    bookId2, snapshot(bookId2, 100, false, false)
            ));

            stubPricingAnyMap(bookId1, 2, true, null, 100);
            stubPricingAnyMap(bookId2, 1, true, null, 100);

            CartItemsResponseDto res = cartService.getUserCart(userId);

            assertThat(res.getItems()).hasSize(2);
            assertThat(res.getTotalItemCount()).isEqualTo(2);
            assertThat(res.getTotalQuantity()).isEqualTo(3);
            assertThat(res.getTotalPrice()).isEqualTo(1500 * 2 + 1500 * 1);
            // selectedTotalPrice는 bookId1만 selected=true
            assertThat(res.getSelectedQuantity()).isEqualTo(2);
            assertThat(res.getSelectedTotalPrice()).isEqualTo(1500 * 2);

            verify(cartRepository, never()).findByUserId(anyLong());
            verify(cartItemRepository, never()).findByCart(any());
        }

        @Test
        void redisEmpty_dirtyTrue_returnsEmpty_andBlocksDbFallback() {
            long userId = 1L;

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());
            given(cartRedisRepository.isUserCartDirty(userId)).willReturn(true);

            CartItemsResponseDto res = cartService.getUserCart(userId);

            assertThat(res.getItems()).isEmpty();
            verifyNoInteractions(cartRepository, cartItemRepository, bookServiceClient, cartCalculator);
        }

        @Test
        void redisEmpty_dirtyFalse_cartMissing_returnsEmpty() {
            long userId = 1L;
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());
            given(cartRedisRepository.isUserCartDirty(userId)).willReturn(false);
            given(cartRepository.findByUserId(userId)).willReturn(Optional.empty());

            CartItemsResponseDto res = cartService.getUserCart(userId);

            assertThat(res.getItems()).isEmpty();
            verify(cartItemRepository, never()).findByCart(any());
        }

        @Test
        void redisEmpty_dirtyFalse_dbFallback_warmsUpCache_andBuildsResponse() {
            long userId = 1L;
            long bookId = 10L;

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());
            given(cartRedisRepository.isUserCartDirty(userId)).willReturn(false);

            Cart cart = Cart.builder().userId(userId).build();
            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

            CartItem db = dbItem(cart, bookId, 2, true);
            given(cartItemRepository.findByCart(cart)).willReturn(List.of(db));

            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 100, false, false)));
            stubPricingAnyMap(bookId, 2, true, null, 100);

            CartItemsResponseDto res = cartService.getUserCart(userId);

            assertThat(res.getItems()).hasSize(1);
            verify(cartRedisRepository).clearUserCart(userId);
            verify(cartRedisRepository, atLeastOnce()).putUserItem(eq(userId), any(CartRedisItem.class));
        }
    }

    // ============================================================
    // getUserSelectedCart / filterSelectedOnly branches
    // ============================================================
    @Nested
    class SelectedOnly {

        @Test
        void getUserSelectedCart_whenFullEmpty_returnEmpty() {
            long userId = 1L;
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());
            given(cartRedisRepository.isUserCartDirty(userId)).willReturn(false);
            given(cartRepository.findByUserId(userId)).willReturn(Optional.empty());

            CartItemsResponseDto res = cartService.getUserSelectedCart(userId);

            assertThat(res.getItems()).isEmpty();
            assertThat(res.getTotalItemCount()).isZero();
        }

        @Test
        void getUserSelectedCart_filtersSelectedAndAvailable_only() {
            long userId = 1L;
            long b1 = 10L; // selected+available
            long b2 = 11L; // selected but unavailable
            long b3 = 12L; // not selected

            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(
                            b1, redisItem(b1, 2, true),
                            b2, redisItem(b2, 1, true),
                            b3, redisItem(b3, 5, false)
                    ));

            stubSnapshots(List.of(b1, b2, b3), Map.of(
                    b1, snapshot(b1, 100, false, false),
                    b2, snapshot(b2, 0, false, false),   // stock 0 -> pricing에서 unavailable로 내려도 됨
                    b3, snapshot(b3, 100, false, false)
            ));

            stubPricingAnyMap(b1, 2, true, null, 100);
            stubPricingAnyMap(b2, 1, false, CartItemUnavailableReason.OUT_OF_STOCK, 0);
            stubPricingAnyMap(b3, 5, true, null, 100);

            CartItemsResponseDto res = cartService.getUserSelectedCart(userId);

            assertThat(res.getItems()).hasSize(1);
            assertThat(res.getItems().get(0).getBookId()).isEqualTo(b1);
            assertThat(res.getTotalQuantity()).isEqualTo(2);
            assertThat(res.getTotalPrice()).isEqualTo(1500 * 2);
        }
    }

    // ============================================================
    // getUserCartCount branches (cache hit / db fallback / warm-up)
    // ============================================================
    @Nested
    class Count {

        @Test
        void getUserCartCount_whenCacheHit_returnsCounts_withoutDb() {
            long userId = 1L;

            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(
                            10L, redisItem(10L, 2, true),
                            11L, redisItem(11L, 5, false)
                    ));

            CartItemCountResponseDto res = cartService.getUserCartCount(userId);

            assertThat(res.getItemCount()).isEqualTo(2);
            assertThat(res.getTotalQuantity()).isEqualTo(7);

            verifyNoInteractions(cartRepository, cartItemRepository);
        }

        @Test
        void getUserCartCount_whenCacheEmpty_cartMissing_returnsZero() {
            long userId = 1L;
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());
            given(cartRepository.findByUserId(userId)).willReturn(Optional.empty());

            CartItemCountResponseDto res = cartService.getUserCartCount(userId);

            assertThat(res.getItemCount()).isZero();
            assertThat(res.getTotalQuantity()).isZero();
        }

        @Test
        void getUserCartCount_whenDbFallback_warmsUpCache() {
            long userId = 1L;
            long bookId = 10L;

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());

            Cart cart = Cart.builder().userId(userId).build();
            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

            CartItem db = dbItem(cart, bookId, 3, true);
            given(cartItemRepository.findByCart(cart)).willReturn(List.of(db));

            CartItemCountResponseDto res = cartService.getUserCartCount(userId);

            assertThat(res.getItemCount()).isEqualTo(1);
            assertThat(res.getTotalQuantity()).isEqualTo(3);

            verify(cartRedisRepository).clearUserCart(userId);
            verify(cartRedisRepository).putUserItem(eq(userId), any(CartRedisItem.class));
        }
    }

    // ============================================================
    // Merge branches
    // ============================================================
    @Nested
    class Merge {

        @Test
        void merge_whenGuestEmpty_returnsEmptySucceededTrue() {
            long userId = 1L;
            String uuid = "g1";

            given(cartRedisRepository.getGuestCartItems(uuid)).willReturn(Collections.emptyMap());

            CartMergeResultResponseDto res = cartService.mergeGuestCartToUserCart(userId, uuid);

            assertThat(res.isMergeSucceeded()).isTrue();
            assertThat(res.getMergedItems()).isEmpty();
            assertThat(res.getFailedToMergeItems()).isEmpty();
            assertThat(res.getExceededMaxQuantityItems()).isEmpty();
            assertThat(res.getUnavailableItems()).isEmpty();

            verify(cartRedisRepository, never()).clearGuestCart(uuid);
            verify(cartRedisRepository, never()).putUserItem(anyLong(), any());
        }

        @Test
        void merge_whenUserRedisEmpty_warmUpFromDb_thenMerge() {
            long userId = 1L;
            String uuid = "g1";
            long bookId = 10L;

            given(cartRedisRepository.getGuestCartItems(uuid))
                    .willReturn(Map.of(bookId, redisItem(bookId, 2, true)));

            Cart cart = Cart.builder().userId(userId).build();
            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));
            given(cartItemRepository.findByCart(cart))
                    .willReturn(List.of(dbItem(cart, 99L, 1, true))); // warm-up 용 더미

            // ✅ 여기서 단 1번만:
            // 1) merge 시작: empty → warm-up 진입
            // 2) warm-up 직후(다시 조회할 수 있음): empty로 둬도 됨
            // 3) merge 끝나고 응답 빌드용: merged map(qty=2)
            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Collections.emptyMap())
                    .willReturn(Collections.emptyMap())
                    .willReturn(Map.of(bookId, redisItem(bookId, 2, true)));

            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 100, false, false)));
            stubPricingAnyMap(bookId, 2, true, null, 100);

            CartMergeResultResponseDto res = cartService.mergeGuestCartToUserCart(userId, uuid);

            assertThat(res.isMergeSucceeded()).isTrue();
            verify(cartRedisRepository).clearGuestCart(uuid);
            verify(cartRedisRepository, atLeastOnce()).putUserItem(eq(userId), any(CartRedisItem.class));
            verify(cartRedisRepository, atLeastOnce()).markUserCartDirty(userId);
        }


        @Test
        void merge_whenSizeLimitExceeded_addFailedToMerge_andDoNotPut() {
            long userId = 1L;
            String uuid = "g1";

            Map<Long, CartRedisItem> guest = Map.of(999L, redisItem(999L, 1, true));
            given(cartRedisRepository.getGuestCartItems(uuid)).willReturn(guest);

            // user already at MAX_CART_SIZE distinct
            Map<Long, CartRedisItem> userMap = new HashMap<>();
            for (int i = 0; i < MAX_CART_SIZE; i++) {
                userMap.put((long) (1000 + i), redisItem(1000 + i, 1, true));
            }
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(userMap);

            // snapshots empty도 허용(safeGetBookSnapshots 실패 케이스) -> unavailable 판정으로 들어갈 수 있으나
            // 여기서는 사이즈 제한이 먼저 걸려 continue 하므로 스냅샷 영향 없음.
            given(bookServiceClient.getBookSnapshots(anyList())).willReturn(Collections.emptyMap());

            // mergedUserItems를 build할 때도 getUserCartItems를 호출하므로, 그대로 반환
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(userMap);
            given(cartCalculator.calculatePricing(anyLong(), anyInt(), anyMap()))
                    .willAnswer(inv -> {
                        Long bookId = inv.getArgument(0);
                        Integer qty = inv.getArgument(1);
                        // lineTotal = 1500 * qty 로만 맞춰서 NPE 방지 + 합계 계산 가능
                        return new CartCalculator.CartItemPricingResult(
                                "t-" + bookId,
                                "u-" + bookId,
                                2000,
                                1500,
                                999,
                                false,
                                true,
                                null,
                                1500 * qty
                        );
                    });


            // pricing 스텁은 필요 없음(mergedUserItems에는 guestBookId가 안 들어가므로)

            CartMergeResultResponseDto res = cartService.mergeGuestCartToUserCart(userId, uuid);

            assertThat(res.isMergeSucceeded()).isTrue();
            assertThat(res.getFailedToMergeItems()).hasSize(1);
            verify(cartRedisRepository, never()).putUserItem(eq(userId), argThat(it -> Objects.equals(it.getBookId(), 999L)));
        }

        @Test
        void merge_whenRequestedOverMaxQuantity_recordsExceededList_andCapsQuantity() {
            long userId = 1L;
            String uuid = "g1";
            long bookId = 10L;

            // guest + user same bookId => 합산이 MAX 초과
            given(cartRedisRepository.getGuestCartItems(uuid))
                    .willReturn(Map.of(bookId, redisItem(bookId, 60, true)));

            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(bookId, redisItem(bookId, 60, true)));

            // stock 충분
            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 999, false, false)));

            // merge 후 조회(응답 빌드)용
            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(bookId, redisItem(bookId, MAX_QUANTITY, true)));

            stubPricingAnyMap(bookId, MAX_QUANTITY, true, null, 999);

            CartMergeResultResponseDto res = cartService.mergeGuestCartToUserCart(userId, uuid);

            assertThat(res.getExceededMaxQuantityItems()).isNotEmpty();
            verify(cartRedisRepository).putUserItem(eq(userId), argThat(it ->
                    it.getBookId().equals(bookId) && it.getQuantity() == MAX_QUANTITY
            ));
        }

        @Test
        void merge_whenStockLimitExceeded_finalQuantityReduced_andIssueRecorded() {
            long userId = 1L;
            String uuid = "g1";
            long bookId = 10L;

            given(cartRedisRepository.getGuestCartItems(uuid))
                    .willReturn(Map.of(bookId, redisItem(bookId, 5, true)));

            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(bookId, redisItem(bookId, 5, true)));

            // requested 10, stock 7 => final 7 with STOCK_LIMIT_EXCEEDED
            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 7, false, false)));

            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(bookId, redisItem(bookId, 7, true)));

            stubPricingAnyMap(bookId, 7, true, null, 7);

            CartMergeResultResponseDto res = cartService.mergeGuestCartToUserCart(userId, uuid);

            assertThat(res.getExceededMaxQuantityItems()).isNotEmpty();
            verify(cartRedisRepository).putUserItem(eq(userId), argThat(it -> it.getQuantity() == 7));
        }

        @Test
        void merge_whenUnavailable_addUnavailableList_butStillMerged() {
            long userId = 1L;
            String uuid = "g1";
            long bookId = 10L;

            given(cartRedisRepository.getGuestCartItems(uuid))
                    .willReturn(Map.of(bookId, redisItem(bookId, 1, true)));

            // ✅ 여기서 단 1번만. (merge 시작 시점엔 empty, merge 끝나고 응답 빌드 때는 merged map)
            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Collections.emptyMap())
                    .willReturn(Map.of(bookId, redisItem(bookId, 1, true)));

            // deleted => unavailable
            stubSnapshots(List.of(bookId), Map.of(bookId, snapshot(bookId, 10, true, false)));

            // ✅ qty=1로 호출되어야 함 (위 stubbing이 덮이지 않으면 1로 유지)
            stubPricingAnyMap(bookId, 1, false, CartItemUnavailableReason.BOOK_DELETED, 10);

            CartMergeResultResponseDto res = cartService.mergeGuestCartToUserCart(userId, uuid);

            assertThat(res.getUnavailableItems()).hasSize(1);
            verify(cartRedisRepository).putUserItem(eq(userId), any(CartRedisItem.class));
            verify(cartRedisRepository).clearGuestCart(uuid);
        }

    }

    // ============================================================
    // Scheduler branches
    // ============================================================
    @Nested
    class Scheduler {

        @Test
        void flushDirtyUserCarts_whenDirtyNull_end() {
            given(cartRedisRepository.getDirtyUserIds()).willReturn(null);

            cartService.flushDirtyUserCarts();

            verify(cartRepository, never()).findByUserId(anyLong());
            verify(cartItemRepository, never()).findByCart(any());
        }

        @Test
        void flushDirtyUserCarts_whenHasDirty_callsFlushSinglePerUser() {
            // flushSingleUserCart 내부를 통과시키려면 최소 스텁 필요
            given(cartRedisRepository.getDirtyUserIds()).willReturn(Set.of(1L, 2L));
            given(cartRedisRepository.getUserCartItems(anyLong())).willReturn(Collections.emptyMap());

            // cart 생성/조회 경로를 간단히 empty redis => deleteAllInBatch 분기 타게 구성
            given(cartRepository.findByUserId(anyLong())).willReturn(Optional.of(Cart.builder().userId(1L).build()));
            given(cartItemRepository.findByCart(any())).willReturn(Collections.emptyList());

            cartService.flushDirtyUserCarts();

            verify(cartRedisRepository, atLeast(2)).clearUserCartDirty(anyLong());
        }

        @Test
        void flushSingleUserCart_whenRedisEmptyAndDbNotEmpty_deleteAllAndClearDirty() {
            long userId = 1L;

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(Collections.emptyMap());

            Cart cart = Cart.builder().userId(userId).build();
            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

            CartItem db1 = dbItem(cart, 10L, 1, true);
            CartItem db2 = dbItem(cart, 11L, 1, true);
            given(cartItemRepository.findByCart(cart)).willReturn(List.of(db1, db2));

            cartService.flushSingleUserCart(userId);

            verify(cartItemRepository).deleteAllInBatch(anyList());
            verify(cartRedisRepository).clearUserCartDirty(userId);
        }

        @Test
        void flushSingleUserCart_whenRedisNull_treatedAsEmpty_andClearsDirty() {
            long userId = 1L;

            given(cartRedisRepository.getUserCartItems(userId)).willReturn(null);

            Cart cart = Cart.builder().userId(userId).build();
            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));
            given(cartItemRepository.findByCart(cart)).willReturn(Collections.emptyList());

            cartService.flushSingleUserCart(userId);

            verify(cartRedisRepository).clearUserCartDirty(userId);
            verify(cartItemRepository, never()).saveAll(anyList());
        }

        @Test
        void flushSingleUserCart_whenRedisHasItems_deletesDbOnlyItems_andUpsertsChangedOnly() {
            long userId = 1L;

            // redis has bookId 10(qty2), 12(qty1)
            Map<Long, CartRedisItem> redis = new HashMap<>();
            redis.put(10L, redisItem(10L, 2, true));
            redis.put(12L, redisItem(12L, 1, false));
            given(cartRedisRepository.getUserCartItems(userId)).willReturn(redis);

            Cart cart = Cart.builder().userId(userId).build();
            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

            // DB has bookId 10(qty1 changed), 11(should be deleted)
            CartItem db10 = dbItem(cart, 10L, 1, true);
            CartItem db11 = dbItem(cart, 11L, 1, true);
            given(cartItemRepository.findByCart(cart)).willReturn(List.of(db10, db11));

            cartService.flushSingleUserCart(userId);

            verify(cartItemRepository).deleteByCartAndBookIdIn(eq(cart), argThat(list -> list.size() == 1 && list.contains(11L)));
            verify(cartItemRepository).saveAll(anyList()); // changed(10) + new(12)
            verify(cartRedisRepository).clearUserCartDirty(userId);
        }

        @Test
        void flushSingleUserCart_whenNoChanges_saveAllNotCalled() {
            long userId = 1L;

            // redis has bookId 10 qty=1 selected=true (same as DB)
            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(10L, redisItem(10L, 1, true)));

            Cart cart = Cart.builder().userId(userId).build();
            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

            CartItem db10 = dbItem(cart, 10L, 1, true);
            given(cartItemRepository.findByCart(cart)).willReturn(List.of(db10));

            cartService.flushSingleUserCart(userId);

            verify(cartItemRepository, never()).saveAll(anyList());
            verify(cartRedisRepository).clearUserCartDirty(userId);
        }

        @Test
        void flushSingleUserCart_whenCartNotExist_createAndSave() {
            long userId = 1L;

            given(cartRedisRepository.getUserCartItems(userId))
                    .willReturn(Map.of(10L, redisItem(10L, 1, true)));

            given(cartRepository.findByUserId(userId)).willReturn(Optional.empty());

            Cart saved = Cart.builder().userId(userId).build();
            given(cartRepository.save(any(Cart.class))).willReturn(saved);
            given(cartItemRepository.findByCart(saved)).willReturn(Collections.emptyList());

            cartService.flushSingleUserCart(userId);

            verify(cartRepository).save(any(Cart.class));
            verify(cartItemRepository).saveAll(anyList()); // 신규 insert
            verify(cartRedisRepository).clearUserCartDirty(userId);
        }
    }
}
