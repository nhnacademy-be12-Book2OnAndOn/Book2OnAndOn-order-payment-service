package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.service;

import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.MAX_CART_SIZE;
import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.MAX_QUANTITY;
import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.MIN_QUANTITY;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemQuantityUpdateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectAllRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.MergeIssueItemDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartMergeResultResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.Cart;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartRedisItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.MergeIssueReason;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartErrorCode;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartItemNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository.CartItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository.CartRedisRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository.CartRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.client.BookServiceClient.BookSnapshot;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.support.CartCalculator;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.support.CartCalculator.CartItemPricingResult;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRedisRepository cartRedisRepository;
    private final BookServiceClient bookServiceClient;
    private final CartCalculator cartCalculator;
    // 배송 정책을 위한 임시 인터페이스
//    private final DeliveryPolicyService deliveryPolicyService;

    // ==========================
    // Scheduler
    // ==========================

    // 일정 주기로 dirty user cart를 DB와 동기화.
    // ShedLock으로 멀티 인스턴스 동시 실행 방지.
    @Scheduled(fixedDelayString = "${cart.flush-interval-ms:60000}")
    @SchedulerLock(name = "cartFlushScheduler", lockAtMostFor = "2m")
    @Transactional
    public void flushDirtyUserCarts() {
        Set<Long> dirtyUserIds = cartRedisRepository.getDirtyUserIds();
        if (dirtyUserIds == null || dirtyUserIds.isEmpty()) {
            return;
        }

        for (Long userId : dirtyUserIds) {
            flushSingleUserCart(userId);
        }
    }

    private void flushSingleUserCart(Long userId) {
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getUserCartItems(userId);

        // Cart 엔티티 확보 또는 생성
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userId(userId).build()
                ));

        // 기존 CartItem 전부 삭제
        List<CartItem> existingItems = cartItemRepository.findByCart(cart);
        if (!existingItems.isEmpty()) {
            cartItemRepository.deleteAllInBatch(existingItems);
        }

        // Redis가 비어있다면, DB도 비운 상태로 끝
        if (redisItems == null || redisItems.isEmpty()) {
            cartRedisRepository.clearUserCartDirty(userId);
            return;
        }

        // Redis 내용을 기준으로 DB CartItem 재구성
        List<CartItem> newItems = redisItems.values().stream()
                .map(ri -> CartItem.builder()
                        .cart(cart)
                        .bookId(ri.getBookId())
                        .quantity(ri.getQuantity())
                        .selected(ri.isSelected())
                        .build()
                )
                .collect(Collectors.toList());

        cartItemRepository.saveAll(newItems);
        cartRedisRepository.clearUserCartDirty(userId);
    }


    // ==========================
    // 회원 장바구니 (DB + Redis 캐시)
    // ==========================

    // 1. 회원 장바구니 전체 목록 조회 (Redis 우선 읽기)
    @Override
    @Transactional(readOnly = true)
    public CartItemsResponseDto getUserCart(Long userId) {
        // 회원 Redis 카트 우선 조회 (로그인 여부와는 별개로, 장바구니 기능이 움직이기 시작하면 Redis에도 데이터가 쌓인다)
        Map<Long, CartRedisItem> cachedItems = cartRedisRepository.getUserCartItems(userId);
        if (cachedItems != null && !cachedItems.isEmpty()) {
            // 캐시 기반으로 바로 응답 구성
            return buildCartItemsResponseFromRedis(cachedItems);
        }

        // Redis에 없으면 DB -> Redis 초기화
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0, 0, 0, 0, 0
            );
        }

        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        // DB 결과를 Redis에 싱크(캐시 warm-up = 데이터 미리 채워넣기)
        syncUserCartCache(userId, cartItems);
        // 응답 반환 (DB 반환)
        return buildCartItemsResponse(cartItems);
    }

    // 2. 회원 장바구니 담기 – Redis만 수정 + dirty
    @Override
    public void addItemToUserCart(Long userId, CartItemRequestDto requestDto) {

        validateQuantity(requestDto.getQuantity());

        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        CartRedisItem existingItem = userItems.get(requestDto.getBookId());
        boolean isNewItem = (existingItem == null);

        if (isNewItem && (userItems.size() >= MAX_CART_SIZE)) {
            throw new CartBusinessException(
                    CartErrorCode.CART_SIZE_EXCEEDED,
                    "최대 " + MAX_CART_SIZE + "종의 상품만 담을 수 있습니다."
            );
        }

        int baseQuantity = (existingItem != null) ? existingItem.getQuantity() : 0;
        int requestedTotal = baseQuantity + requestDto.getQuantity();  // 기존 + 신규
        int capped = capQuantity(requestedTotal);  // 시스템 상한(예: 99) 적용

        BookSnapshot snapshot = requireBookSnapshot(requestDto.getBookId());
        validateBookAvailableForCart(snapshot);
        validateStockForQuantity(snapshot, capped);  // 실제로 저장할 수량 기준 검증

        CartRedisItem newItem;

        if (existingItem != null) {
            existingItem.setQuantity(capped);
            existingItem.setSelected(requestDto.isSelected());
            existingItem.touchUpdatedAt();
            newItem = existingItem;
        } else {
            newItem = CartRedisItem.newItem(
                    requestDto.getBookId(),
                    capped,
                    requestDto.isSelected()
            );
        }

        cartRedisRepository.putUserItem(userId, newItem);
        cartRedisRepository.markUserCartDirty(userId);
    }

    // 3. 회원 장바구니 내에서 단건 상품 수량 변경
    @Override
    public void updateUserItemQuantity(Long userId, CartItemQuantityUpdateRequestDto requestDto) {
        validateQuantity(requestDto.getQuantity());

        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        CartRedisItem existingItem = userItems.get(requestDto.getBookId());
        if (existingItem == null) {
            throw new CartItemNotFoundException(
                    CartErrorCode.CART_ITEM_NOT_FOUND,
                    "Cart item을 찾을 수 없습니다. bookId=" + requestDto.getBookId()
            );
        }

        int capped = capQuantity(requestDto.getQuantity());

        BookSnapshot snapshot = requireBookSnapshot(requestDto.getBookId());
        validateBookAvailableForCart(snapshot);
        validateStockForQuantity(snapshot, capped);

        existingItem.setQuantity(capped);
        existingItem.touchUpdatedAt();

        cartRedisRepository.putUserItem(userId, existingItem);
        cartRedisRepository.markUserCartDirty(userId);
    }

    // 4. 회원 장바구니 단건 상품 제거
    @Override
    public void removeItemFromUserCart(Long userId, CartItemDeleteRequestDto requestDto) {
        cartRedisRepository.deleteUserCartItem(userId, requestDto.getBookId());
        cartRedisRepository.markUserCartDirty(userId);
    }

    // 5. 회원 장바구니 특정 상품 선택/해제
    @Override
    public void selectUserCartItem(Long userId, CartItemSelectRequestDto requestDto) {
        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        CartRedisItem existingItem = userItems.get(requestDto.getBookId());
        if (existingItem == null) {
            throw new CartItemNotFoundException(
                    CartErrorCode.CART_ITEM_NOT_FOUND,
                    "Cart item을 찾을 수 없습니다. bookId=" + requestDto.getBookId()
            );
        }

        existingItem.setSelected(requestDto.isSelected());
        existingItem.touchUpdatedAt();
        cartRedisRepository.putUserItem(userId, existingItem);
        cartRedisRepository.markUserCartDirty(userId);
    }

    // 6. 회원 장바구니에서 선택된 모든 항목 제거
    @Override
    public void deleteSelectedUserCartItem(Long userId) {
        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        if (userItems == null || userItems.isEmpty()) {
            return;
        }
        for (CartRedisItem item : userItems.values()) {
            if (item.isSelected()) {
                cartRedisRepository.deleteUserCartItem(userId, item.getBookId());
            }
        }
        cartRedisRepository.markUserCartDirty(userId);
    }

    // 7. 회원 장바구니 상품 전체 선택/해제
    @Override
    public void selectAllUserCartItems(Long userId, CartItemSelectAllRequestDto requestDto) {
        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        if (userItems == null || userItems.isEmpty()) {
            return;
        }
        for (CartRedisItem item : userItems.values()) {
            item.setSelected(requestDto.isSelected());
            item.touchUpdatedAt(); // 시간 갱신
            cartRedisRepository.putUserItem(userId, item);
        }
        cartRedisRepository.markUserCartDirty(userId);
    }

    // 8. 회원 장바구니 전체 항목 비우기(결제 완료 시)
    // -> 주문 서비스와 연동하면서 “결제 성공 시 clearGuestCart 호출”로 연결
    @Override
    public void clearUserCart(Long userId) {
        cartRedisRepository.clearUserCart(userId); // Redis에서 전체 삭제
        cartRedisRepository.markUserCartDirty(userId); // DB에서도 비워야 함
    }

    // 9. 회원 장바구니에 담긴 품목 개수 조회 (헤더 아이콘용)
    @Override
    @Transactional(readOnly = true)
    public CartItemCountResponseDto getUserCartCount(Long userId) {
        // 1) 캐시에서 먼저 개수 계산 (book-service 호출 없음)
        Map<Long, CartRedisItem> cachedItems = cartRedisRepository.getUserCartItems(userId);
        if (cachedItems != null && !cachedItems.isEmpty()) {
            // 캐시 기반으로 바로 응답 구성
            int itemCount = cachedItems.size();
            int totalQuantity = cachedItems.values().stream()
                    .mapToInt(CartRedisItem::getQuantity)
                    .sum();
            return new CartItemCountResponseDto(itemCount, totalQuantity);
        }
        // 캐시가 비어 있으면 0,0 반환 (또는 DB 기준 계산 로직 추가 가능)
        return new CartItemCountResponseDto(0,0);
    }

    // 10. 회원 장바구니 선택된 항목 주문
    @Override
    @Transactional(readOnly = true)
    public CartItemsResponseDto getUserSelectedCart(Long userId) {
        // 1) 회원용 전체 장바구니 먼저 조회 (캐시/DB 로직 재사용)
        CartItemsResponseDto full = getUserCart(userId);

        // 2) 선택된 항목만 필터링해서 새 DTO 생성
        return filterSelectedOnly(full);
    }

    // ==========================
    // 비회원(guest) 장바구니 (Redis + uuid)
    // ==========================
    // 1. 비회원 장바구니 전체 목록 조회
    @Override
    @Transactional(readOnly = true)
    public CartItemsResponseDto getGuestCart(String uuid) {
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        return buildCartItemsResponseFromRedis(redisItems);
    }

    // 2. 비회원 장바구니 상품 추가 (수량 +1 또는 신규 추가)
    @Override
    public void addItemToGuestCart(String uuid, CartItemRequestDto requestDto) {
        validateQuantity(requestDto.getQuantity());

        // 1) Redis 데이터 조회
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);

        CartRedisItem existing = redisItems.get(requestDto.getBookId());
        boolean isNewItem = (existing == null);

        // 2) 장바구니 사이즈 제한 검증 (신규 항목일 경우)
        if (isNewItem && redisItems.size() >= MAX_CART_SIZE) {
            throw new CartBusinessException(
                    CartErrorCode.CART_SIZE_EXCEEDED,
                    "최대 " + MAX_CART_SIZE + "종의 상품만 담을 수 있습니다."
            );
        }

        // 3) 수량 계산 및 상품 유효성 검증
        int baseQuantity = (existing != null) ? existing.getQuantity() : 0;
        int requestedTotal = baseQuantity + requestDto.getQuantity();
        int capped = capQuantity(requestedTotal);

        // 4) 도서 스냅샷 조회 및 상태/재고 검증
        BookSnapshot snapshot = requireBookSnapshot(requestDto.getBookId());
        validateBookAvailableForCart(snapshot);
        validateStockForQuantity(snapshot, capped);

        // 5) 실제 Redis 반영
        cartRedisRepository.updateGuestItemQuantity(uuid, requestDto.getBookId(), capped);
    }

    // 3. 비회원 장바구니 내에서 단건 상품 수량 변경
    @Override
    public void updateGuestItemQuantity(String uuid, CartItemQuantityUpdateRequestDto requestDto) {
        validateQuantity(requestDto.getQuantity());

        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        CartRedisItem existing = redisItems.get(requestDto.getBookId());

        if (existing == null) {
            throw new CartItemNotFoundException(
                    CartErrorCode.CART_ITEM_NOT_FOUND,
                    "장바구니에 존재하지 않는 상품입니다. bookId=" + requestDto.getBookId()
            );
        }

        int capped = capQuantity(requestDto.getQuantity());

        BookSnapshot snapshot = requireBookSnapshot(requestDto.getBookId());
        validateBookAvailableForCart(snapshot);
        validateStockForQuantity(snapshot, capped);

        existing.setQuantity(capped);
        existing.setUpdatedAt(System.currentTimeMillis());
        cartRedisRepository.putGuestItem(uuid, existing);
    }

    // 4. 비회원 장바구니 단건 상품 제거
    @Override
    public void removeItemFromGuestCart(String uuid, CartItemDeleteRequestDto requestDto) {
        cartRedisRepository.deleteGuestItem(uuid, requestDto.getBookId());
    }

    // 5. 비회원 장바구니 특정 상품 선택/해제
    @Override
    public void selectGuestCartItem(String uuid, CartItemSelectRequestDto requestDto) {
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        CartRedisItem item = redisItems.get(requestDto.getBookId());
        if (item == null) {
            throw new CartItemNotFoundException(
                    CartErrorCode.CART_ITEM_NOT_FOUND,
                    "비회원 장바구니의 아이템을 찾을 수 없습니다. bookId=" + requestDto.getBookId()
            );
        }

        item.setSelected(requestDto.isSelected());
        cartRedisRepository.putGuestItem(uuid, item);
    }

    // 6. 비회원 장바구니 선택된 항목 제거
    @Override
    public void deleteSelectedGuestCartItem(String uuid) {
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        if (redisItems == null || redisItems.isEmpty()) {
            return;
        }
        for (CartRedisItem item : redisItems.values()) {
            if (item.isSelected()) {
                cartRedisRepository.deleteGuestItem(uuid, item.getBookId());
            }
        }
    }

    // 7. 비회원 장바구니 상품 전체 선택/해제
    @Override
    public void selectAllGuestCartItems(String uuid, CartItemSelectAllRequestDto requestDto) {
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        boolean selected = requestDto.isSelected();

        for (CartRedisItem item : redisItems.values()) {
            item.setSelected(selected);
            cartRedisRepository.putGuestItem(uuid, item);
        }
    }

    // 8. 비회원 장바구니 전체 항목 비우기 (로그인 후 병합 완료 시)
    @Override
    public void clearGuestCart(String uuid) {
        cartRedisRepository.clearGuestCart(uuid);
    }

    // 9. 비회원 장바구니의 품목 개수 조회
    @Override
    @Transactional(readOnly = true)
    public CartItemCountResponseDto getGuestCartCount(String uuid) {
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);

        if (redisItems == null || redisItems.isEmpty()) {
            return new CartItemCountResponseDto(0,0);
        }

        int itemCount = redisItems.size();
        int totalQuantity = redisItems.values().stream()
                .mapToInt(CartRedisItem::getQuantity)
                .sum();
        return new CartItemCountResponseDto(itemCount, totalQuantity);
    }

    // 10. 비회원 장바구니 선택된 항목 주문
    @Override
    @Transactional(readOnly = true)
    public CartItemsResponseDto getGuestSelectedCart(String uuid) {
        CartItemsResponseDto full = getGuestCart(uuid);
        return filterSelectedOnly(full);
    }

    // ==========================
    // guest(uuid) → user(DB) 병합(Merge)
    // ==========================
    // 1. 비회원 장바구니 데이터를 회원 장바구니(DB)로 병합 처리 (localStorage의 uuid + 로그인 후 userId)
    @Override
    public CartMergeResultResponseDto mergeGuestCartToUserCart(Long userId, String uuid) {

        Map<Long, CartRedisItem> guestItems = cartRedisRepository.getGuestCartItems(uuid);

        if (guestItems == null || guestItems.isEmpty()) {
            return new CartMergeResultResponseDto(
                    Collections.emptyList(), // mergedItems
                    Collections.emptyList(), // failedToMergeItems
                    Collections.emptyList(), // exceededMaxQuantityItems
                    Collections.emptyList(), // unavailableItems
                    true // mergeSucceeded
            );
        }

        // 1) guest 도서 상태 스냅샷
        List<Long> guestBookIds = new ArrayList<>(guestItems.keySet());
        Map<Long, BookSnapshot> guestSnapshotMap = bookServiceClient.getBookSnapshots(guestBookIds);

        // 2) 회원 카트 및 기존 아이템 조회
        Cart userCart = getOrCreateCart(userId);
        List<CartItem> userItems = cartItemRepository.findByCart(userCart);
        Map<Long, CartItem> userItemMap = userItems.stream()
                .collect(Collectors.toMap(CartItem::getBookId, cartItem -> cartItem));

        int currentDistinctCount = userItemMap.size(); // 현재 서로 다른 bookId 개수

        List<MergeIssueItemDto> exceededMaxQuantityItems = new ArrayList<>();
        List<MergeIssueItemDto> unavailableItems = new ArrayList<>();
        List<MergeIssueItemDto> failedToMergeItems = new ArrayList<>();

        for (Map.Entry<Long, CartRedisItem> entry : guestItems.entrySet()) {
            Long bookId = entry.getKey();
            CartRedisItem guestItem = entry.getValue();
            int guestQuantity = guestItem.getQuantity();

            CartItem userItem = userItemMap.get(bookId);
            int originalUserQuantity = (userItem != null) ? userItem.getQuantity() : 0;

            int requestedMergedQuantity = originalUserQuantity + guestQuantity;
            int finalMergedQuantity = Math.min(requestedMergedQuantity, MAX_QUANTITY);

            BookSnapshot snapshot = guestSnapshotMap.get(bookId);
            boolean isUnavailable = isBookUnavailable(snapshot); // 품절/삭제/판매종료/숨김 등

            // 2-1) 장바구니 최대 품목 수 체크 (새로 들어오는 bookId만 품목 수 증가)
            boolean isNewItem = (userItem == null);
            if (isNewItem && currentDistinctCount >= MAX_CART_SIZE) {
                // 용량 제한으로 아예 담을 수 없는 경우 → failedToMerge 목록에 기록
                failedToMergeItems.add(
                        new MergeIssueItemDto(
                                bookId,
                                guestQuantity,
                                originalUserQuantity,
                                originalUserQuantity, // merge 전 상태 유지
                                MergeIssueReason.CART_SIZE_LIMIT_EXCEEDED
                        )
                );
                // Cart에 넣지 않고 continue
                continue;
            }

            // 2-2) 최대 수량 초과 이슈 기록
            MergeIssueReason quantityIssueReason = null;

            if (snapshot != null && snapshot.getStockCount() > 0) {
                int stock = snapshot.getStockCount();
                if (finalMergedQuantity > stock) {
                    finalMergedQuantity = stock;
                    quantityIssueReason = MergeIssueReason.STOCK_LIMIT_EXCEEDED;
                } else if (requestedMergedQuantity > MAX_QUANTITY) {
                    // 재고는 충분하지만 시스템 상한 때문에 잘린 경우
                    quantityIssueReason = MergeIssueReason.EXCEEDED_MAX_QUANTITY;
                }
            } else {
                // 재고 정보 없거나, 품절/판매종료 등인 경우에는
                // MAX_QUANTITY만 적용하고, STOCK_LIMIT_EXCEEDED는 의미 없음.
                if (requestedMergedQuantity > MAX_QUANTITY) {
                    quantityIssueReason = MergeIssueReason.EXCEEDED_MAX_QUANTITY;
                }
            }
            // 이후에 한 번만 push
            if (quantityIssueReason != null) {
                exceededMaxQuantityItems.add(
                        new MergeIssueItemDto(
                                bookId,
                                guestQuantity,
                                originalUserQuantity,
                                finalMergedQuantity,
                                quantityIssueReason
                        )
                );
            }

            // 2-2) Cart에 최종 수량 반영 (정상/품절 모두 동일하게 처리)
            if (userItem != null) {
                userItem.updateQuantity(finalMergedQuantity);
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(userCart)
                        .bookId(bookId)
                        .quantity(finalMergedQuantity)
                        .selected(true)
                        .build();
                cartItemRepository.save(newItem);
                userItemMap.put(bookId, newItem);
                currentDistinctCount++; // 신규 품목 추가
            }

            // 2-3) 최대 수량 초과 이슈 기록
//            if (requestedMergedQuantity > MAX_QUANTITY) {
//                exceededMaxQuantityItems.add(
//                        new MergeIssueItemDto(
//                                bookId,
//                                guestQuantity,
//                                originalUserQuantity,
//                                finalMergedQuantity,
//                                MergeIssueReason.EXCEEDED_MAX_QUANTITY
//                        )
//                );
//            }

            // 2-4) 구매 불가 이슈 기록 (Cart에는 들어간 상태지만, 조회 시 available=false 처리됨)
            if (isUnavailable) {
                unavailableItems.add(
                        new MergeIssueItemDto(
                                bookId,
                                guestQuantity,
                                originalUserQuantity,
                                finalMergedQuantity,
                                MergeIssueReason.UNAVAILABLE
                        )
                );
            }
        }

        // 3) guest cart 삭제
        cartRedisRepository.clearGuestCart(uuid);

        // 4) merge 이후 user 장바구니 응답 구성
        List<CartItem> mergedItems = cartItemRepository.findByCart(userCart);
        CartItemsResponseDto mergedCartDto = buildCartItemsResponse(mergedItems);
        List<CartItemResponseDto> mergedItemDtos = mergedCartDto.getItems();

        // 5) merge 후 회원 캐시 무효화 (다음 조회 시 DB → Redis 재적재)
        cartRedisRepository.clearUserCart(userId);

        return new CartMergeResultResponseDto(
                mergedItemDtos,
                failedToMergeItems,
                exceededMaxQuantityItems,
                unavailableItems,
                true
        );
    }

    // ==========================
    // 내부 유틸 메서드들
    // ==========================
    // 1. 선택 + 구매 가능(available=true) 아이템만 필터
    private CartItemsResponseDto filterSelectedOnly(CartItemsResponseDto responseDto) {
        if (responseDto == null || responseDto.getItems() == null || responseDto.getItems().isEmpty()) {
            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        // 1) 선택 + 구매 가능(available=true) 아이템만 필터
        List<CartItemResponseDto> selectedItems = responseDto.getItems().stream()
                .filter(CartItemResponseDto::isSelected)
                .filter(CartItemResponseDto::isAvailable) // 품절/삭제/판매종료 제외
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        // 2) 합계 다시 계산
        int totalItemCount = selectedItems.size();
        int totalQuantity = selectedItems.stream()
                .mapToInt(CartItemResponseDto::getQuantity)
                .sum();

        int totalPrice = selectedItems.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        // selected 계열은 "전체=선택"으로 간주
        int selectedQuantity = totalQuantity;
        int selectedTotalPrice = totalPrice;

        return new CartItemsResponseDto(
                selectedItems,
                totalItemCount,
                totalQuantity,
                totalPrice,
                selectedQuantity,
                selectedTotalPrice
        );
    }

    // 2. DB 결과를 Redis에 싱크(캐시 warm-up)
    private void syncUserCartCache(Long userId, List<CartItem> cartItems) {
        // 1) 기존 캐시 삭제 후, DB 값으로 다시 채워 넣기 (Cache Invalidation)
        cartRedisRepository.clearUserCart(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            return;
        }
        // 2) 조회 결과를 Redis 캐시에 적재 (write-through 초기화)
        for (CartItem item : cartItems) {
            long updatedAt = (item.getUpdatedAt() != null)
                    ? item.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                    : System.currentTimeMillis(); // null 방어

            CartRedisItem cartRedisItem = new CartRedisItem(
                    item.getBookId(),
                    item.getQuantity(),
                    item.isSelected(),
                    updatedAt,
                    updatedAt
            );
            // 항목별로 Redis Hash에 데이터 삽입
            cartRedisRepository.putUserItem(userId, cartRedisItem);
        }
    }

    // 3. 장바구니 엔티티 관리
    private Cart getOrCreateCart(Long userId) {
        // 해당 userId의 Cart 엔티티를 조회하거나, 없으면 새로 생성하여 반환
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder() // builder 사용은 “필수 필드만 세팅하고 나머지는 안정적으로 초기화”하기 위한 실무적 패턴
                            .userId(userId)
                            .build();
                    // 새로 생성된 Cart를 DB에 저장
                    return cartRepository.save(newCart);
                });
    }

    // 4. 유효성 검사 및 수량 처리
    // 4-1. 요청 수량이 최소/최대 허용 범위를 벗어나는지 검증
    private void validateQuantity(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new CartBusinessException(
                    CartErrorCode.INVALID_QUANTITY,
                    "수량은 " + MIN_QUANTITY + " 에서 " + MAX_QUANTITY + "사이여야 합니다. : " + quantity
            );
        }
    }

    // 4-2. 요청 수량을 MIN_QUANTITY와 MAX_QUANTITY 범위 내로 강제 조정하여 반환
    private int capQuantity(int quantity) {
        return Math.max(MIN_QUANTITY, Math.min(quantity, MAX_QUANTITY));
    }

    // 4-3. 외부 서비스에서 도서 ID에 대한 최신 스냅샷(가격, 재고 등)을 조회하고 반환
    private BookSnapshot requireBookSnapshot(Long bookId) {
        Map<Long, BookSnapshot> map =
                bookServiceClient.getBookSnapshots(Collections.singletonList(bookId)); // “이 리스트는 그냥 전달만 하고 수정되지 않아야 한다”는 의도가 보장되어야 하기 때문에 singletonList 사용
        BookSnapshot snapshot = map.get(bookId);
        if (snapshot == null) {
            throw new CartBusinessException(
                    CartErrorCode.INVALID_BOOK_ID,
                    "유효하지 않은 도서입니다. bookId=" + bookId
            );
        }
        return snapshot;
    }

    // 4-4. 도서 스냅샷을 기반으로 현재 상품이 장바구니에 담을 수 있는 상태인지 검사
    private void validateBookAvailableForCart(BookSnapshot snapshot) {
        if (snapshot == null || isBookUnavailable(snapshot)) {
            throw new CartBusinessException(
                    CartErrorCode.BOOK_UNAVAILABLE,
                    "구매 불가 상태의 도서는 장바구니에 담을 수 없습니다."
            );
        }
    }

    // 4-5. 요청 수량(requestedQuantity)이 현재 재고(Stock)를 초과하는지 검사
    private void validateStockForQuantity(BookSnapshot snapshot, int requestedQuantity) {
        int stock = snapshot.getStockCount();
        if (stock <= 0) {
            throw new CartBusinessException(
                    CartErrorCode.OUT_OF_STOCK,
                    "품절된 도서입니다. 재고 = 0"
            );
        }
        if (requestedQuantity > stock) {
            throw new CartBusinessException(
                    CartErrorCode.OUT_OF_STOCK,
                    "재고 부족: 요청 수량=" + requestedQuantity + ", 재고=" + stock
            );
        }
    }

    // 4-6. 상품 스냅샷의 여러 상태(삭제됨, 판매 종료, 숨김, 재고 0)를 확인하여 구매 불가 상태인지 종합적으로 판단
    private boolean isBookUnavailable(BookSnapshot snapshot) {
        if (snapshot == null) {
            return true;
        }
        if (snapshot.isDeleted()) {
            return true;
        }
        if (snapshot.isSaleEnded()) {
            return true;
        }
        if (snapshot.isHidden()) {
            return true;
        }
        return snapshot.getStockCount() <= 0;
    }

    // ===== 응답 DTO(CartItemsResponseDto) 빌더 =====
    // 1. DB 결과 기반 응답 DTO
    // -> DB (CartItem) 결과를 기반으로 최신 스냅샷(가격, 재고) 정보를 포함한 최종 응답 DTO를 구성
    private CartItemsResponseDto buildCartItemsResponse(List<CartItem> items) {
        // 0) 배송 정책 조회
//        int baseDeliveryFee = deliveryPolicyService.getDeliveryFee();
//        int freeDeliveryThreshold = deliveryPolicyService.getFreeDeliveryThreshold();

        // 1) 빈 장바구니 처리
        if (items == null || items.isEmpty()) {
//            int deliveryFeeApplied = 0;      // 담긴 상품이 없으니 배송비 0
//            int finalPaymentAmount = 0;      // 결제금액도 0

            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0,      // totalItemCount
                    0,      // totalQuantity
                    0,      // totalPrice
                    0,      // selectedQuantity
                    0      // selectedTotalPrice
//                    deliveryFeeApplied,
//                    freeDeliveryThreshold,
//                    finalPaymentAmount
            );
        }

        // 2) 장바구니 항목을 최근 수정일(updatedAt) 기준 내림차순 정렬
        List<CartItem> sortedItems = items.stream()
                .sorted(Comparator.comparing(
                        CartItem::getUpdatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .collect(Collectors.toList());

        // 3) 외부 상품 서비스에서 필요한 모든 BookSnapshot 정보를 일괄 조회
        List<Long> bookIds = sortedItems.stream()
                .map(CartItem::getBookId)
                .distinct()
                .collect(Collectors.toList());

        // 4) book-service에서 BookSnapshot들 일괄 조회
        Map<Long, BookSnapshot> bookSnapshotMap = bookServiceClient.getBookSnapshots(bookIds);

        // 5) 루프 돌면서 Redis 항목 기반으로 DTO + 합계 계산
        List<CartItemResponseDto> itemDtos = new ArrayList<>();
        // 초기변수
        int totalItemCount = sortedItems.size();
        int totalQuantity = 0;
        int totalPrice = 0;
        int selectedQuantity = 0;
        int selectedTotalPrice = 0;

        // 항목별 가격 계산 및 최종 통계 합산
        for (CartItem item : sortedItems) {
            Long bookId = item.getBookId();
            int lineQuantity = item.getQuantity();
            boolean selected = item.isSelected();

            CartItemPricingResult pricing =
                    cartCalculator.calculatePricing(bookId, lineQuantity, bookSnapshotMap);

            totalQuantity += lineQuantity;
            totalPrice += pricing.getLineTotalPrice();

            if (selected && pricing.isAvailable()) {
                selectedQuantity += lineQuantity;
                selectedTotalPrice += pricing.getLineTotalPrice();
            }

            CartItemResponseDto dto = new CartItemResponseDto(
                    bookId,
                    pricing.getTitle(),
                    pricing.getThumbnailUrl(),
                    pricing.getOriginalPrice(),
                    pricing.getPrice(),
                    lineQuantity,
                    selected,
                    pricing.isAvailable(),
                    pricing.getUnavailableReason(),
                    pricing.getStockCount(),
                    pricing.isLowStock()
            );

            itemDtos.add(dto);
        }

        // 6) 배송비 및 최종 결제 금액 계산
//        int deliveryFeeApplied = 0;
//        if (selectedTotalPrice > 0 && selectedTotalPrice < freeDeliveryThreshold) {
            // 선택된 상품 금액이 무료배송 기준 미만이면 배송비 부과
//            deliveryFeeApplied = baseDeliveryFee;
//        }
//        int finalPaymentAmount = selectedTotalPrice + deliveryFeeApplied;

        // 7) 최종 CartItemsResponseDto 객체 생성 및 반환
        return new CartItemsResponseDto(
                itemDtos,
                totalItemCount,
                totalQuantity,
                totalPrice,
                selectedQuantity,
                selectedTotalPrice
//                deliveryFeeApplied,
//                freeDeliveryThreshold,
//                finalPaymentAmount
        );
    }

    // 2. Redis 결과 기반 응답 DTO 구성
    // -> Redis (CartRedisItem) 캐시 결과를 기반으로 최신 스냅샷 정보를 포함한 최종 응답 DTO를 구성
    private CartItemsResponseDto buildCartItemsResponseFromRedis(Map<Long, CartRedisItem> redisItems) {
        // 0) 배송 정책
//        int baseDeliveryFee = deliveryPolicyService.getDeliveryFee();
//        int freeDeliveryThreshold = deliveryPolicyService.getFreeDeliveryThreshold();

        if (redisItems == null || redisItems.isEmpty()) {
//            int deliveryFeeApplied = 0;
//            int finalPaymentAmount = 0;

            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0,      // totalItemCount
                    0,      // totalQuantity
                    0,      // totalPrice
                    0,      // selectedQuantity
                    0      // selectedTotalPrice
//                    deliveryFeeApplied,
//                    freeDeliveryThreshold,
//                    finalPaymentAmount
            );
        }

        // Redis Map의 Key(bookIds)를 추출하여 BookSnapshot 정보 일괄 조회
        // 1) 정렬된 리스트로 변환 (updatedAt 내림차순)
        List<CartRedisItem> sortedItems = redisItems.values().stream()
                .sorted(Comparator.comparingLong(CartRedisItem::getUpdatedAt).reversed())
                .collect(Collectors.toList());

        // 2) BookSnapshot 조회용 bookId 목록
        List<Long> bookIds = sortedItems.stream()
                .map(CartRedisItem::getBookId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, BookSnapshot> bookSnapshotMap = bookServiceClient.getBookSnapshots(bookIds);

        List<CartItemResponseDto> itemDtos = new ArrayList<>();

        int totalItemCount = redisItems.size();
        int totalQuantity = 0;
        int totalPrice = 0;
        int selectedQuantity = 0;
        int selectedTotalPrice = 0;

        // 항목별 가격 계산 및 최종 통계 합산
        // - Redis 항목을 순회하며 DB 기반 빌더와 동일하게 총 수량, 총 가격 등을 계산
        for (CartRedisItem redisItem : sortedItems) {
            Long bookId = redisItem.getBookId();
            int lineQuantity = redisItem.getQuantity();
            boolean selected = redisItem.isSelected();

            CartItemPricingResult pricing =
                    cartCalculator.calculatePricing(bookId, lineQuantity, bookSnapshotMap);

            totalQuantity += lineQuantity;
            totalPrice += pricing.getLineTotalPrice();

            if (selected && pricing.isAvailable()) {
                selectedQuantity += lineQuantity;
                selectedTotalPrice += pricing.getLineTotalPrice();
            }

            CartItemResponseDto dto = new CartItemResponseDto(
                    bookId,
                    pricing.getTitle(),
                    pricing.getThumbnailUrl(),
                    pricing.getOriginalPrice(),
                    pricing.getPrice(),
                    lineQuantity,
                    selected,
                    pricing.isAvailable(),
                    pricing.getUnavailableReason(),
                    pricing.getStockCount(),
                    pricing.isLowStock()
            );

            itemDtos.add(dto);
        }

        // 배송비 및 최종 결제 금액 계산
//        int deliveryFeeApplied = 0;
//        if (selectedTotalPrice > 0 && selectedTotalPrice < freeDeliveryThreshold) {
//            deliveryFeeApplied = baseDeliveryFee;
//        }
//        int finalPaymentAmount = selectedTotalPrice + deliveryFeeApplied;

        return new CartItemsResponseDto(
                itemDtos,
                totalItemCount,
                totalQuantity,
                totalPrice,
                selectedQuantity,
                selectedTotalPrice
//                deliveryFeeApplied,
//                freeDeliveryThreshold,
//                finalPaymentAmount
        );
    }
}
