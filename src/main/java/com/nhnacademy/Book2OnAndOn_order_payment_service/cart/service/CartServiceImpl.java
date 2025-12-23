package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.service;

import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.MAX_CART_SIZE;
import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.MAX_QUANTITY;
import static com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartConstants.MIN_QUANTITY;

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
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.support.CartCalculator;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.support.CartCalculator.CartItemPricingResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.BookServiceClient.BookSnapshot;
import feign.FeignException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRedisRepository cartRedisRepository;
    private final BookServiceClient bookServiceClient;
    private final CartCalculator cartCalculator;

    // ==========================
    // Scheduler
    // ==========================

    // 일정 주기로 dirty user cart를 DB와 동기화.
    // ShedLock으로 멀티 인스턴스 동시 실행 방지.
    @Scheduled(fixedDelayString = "${cart.flush-interval-ms:60000}") // 스케쥴러 시간 1시간 정도로 연장?
    @SchedulerLock(name = "cartFlushScheduler", lockAtMostFor = "2m")
    @Transactional
    public void flushDirtyUserCarts() {

        Set<Long> dirtyUserIds = cartRedisRepository.getDirtyUserIds();
        if (dirtyUserIds == null || dirtyUserIds.isEmpty()) {
            log.info("[Scheduler] dirty user cart 없음 -> 종료");
            return;
        }

        for (Long userId : dirtyUserIds) {
            log.info("[Scheduler] userId={} flushSingleUserCart 실행", userId);
            flushSingleUserCart(userId);
        }

        log.info("[Scheduler] flushDirtyUserCarts 종료");
    }

    @Transactional
    protected void flushSingleUserCart(Long userId) {
        // 장바구니의 모든 아이템 불러오기
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getUserCartItems(userId);
        log.debug("[Scheduler] Redis user cart 아이템 수 - userId={}, size={}",
                userId, redisItems != null ? redisItems.size() : 0);

        // Cart 엔티티 확보 또는 생성
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));

        // DB의 기존 CartItem 전부 삭제 -> 트랜잭션에 의해 삭제와 삽입 사이에 데이터 유실의 위험은 없다.
        List<CartItem> dbItems = cartItemRepository.findByCart(cart);
        Map<Long, CartItem> dbMap = dbItems.stream()
                .collect(Collectors.toMap(CartItem::getBookId, it -> it));

        // Redis가 비어있다면, DB도 비운 상태로 끝 (DB Flush 과정을 안전하게 종료하는 논리)
        if (redisItems == null || redisItems.isEmpty()) {
            log.info("[Scheduler] Redis cart 비어 있음 -> DB 빈 상태 유지 후 dirty 플래그 해제 - userId={}", userId);
            if (!dbItems.isEmpty()) {
                cartItemRepository.deleteAllInBatch(dbItems);
            }
            cartRedisRepository.clearUserCartDirty(userId);
            return;
        }

        // 2) Redis에는 없고 DB에만 있는 항목 -> delete
        List<Long> toDelete = dbMap.keySet().stream()
                .filter(bookId -> !redisItems.containsKey(bookId))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            cartItemRepository.deleteByCartAndBookIdIn(cart, toDelete);
        }

        // Redis 내용을 기준으로 DB CartItem 재구성
        // 3) Redis에 있는 항목 -> DB upsert(변경된 것만 update, 없는 것은 insert)
        List<CartItem> toSave = new ArrayList<>();
        for (CartRedisItem ri : redisItems.values()) {
            CartItem existing = dbMap.get(ri.getBookId());
            if (existing == null) {
                toSave.add(CartItem.builder()
                        .cart(cart)
                        .bookId(ri.getBookId())
                        .quantity(ri.getQuantity())
                        .selected(ri.isSelected())
                        .build());
            } else {
                boolean changed = (existing.getQuantity() != ri.getQuantity()) || (existing.isSelected() != ri.isSelected());
                if (changed) {
                    existing.updateQuantity(ri.getQuantity());
                    existing.setSelected(ri.isSelected());
                    toSave.add(existing);
                }
            }
        }
        if (!toSave.isEmpty()) {
            cartItemRepository.saveAll(toSave);
        }
        cartRedisRepository.clearUserCartDirty(userId);

        log.info("[Scheduler] flushSingleUserCart 완료 - userId={}, newItemCount={}", userId, toSave.size());
    }


    // ==========================
    // 회원 장바구니 (DB + Redis 캐시)
    // ==========================

    // 1. 회원 장바구니 전체 목록 조회 (Redis 우선 읽기)
    @Override
    @Transactional(readOnly = true)
    public CartItemsResponseDto getUserCart(Long userId) {
        log.info("[UserCart] getUserCart 호출 - userId={}", userId);

        // 회원 Redis 카트 우선 조회
        // -> 로그인 여부와는 별개로, 장바구니 기능이 움직이기 시작하면 Redis에도 데이터가 쌓인다
        Map<Long, CartRedisItem> cachedItems = cartRedisRepository.getUserCartItems(userId);
        if (cachedItems != null && !cachedItems.isEmpty()) {
            log.info("[UserCart] Redis 캐시 히트 - userId={}, itemCount={}", userId, cachedItems.size());
            // 캐시 기반으로 바로 응답 구성
            return buildCartItemsResponseFromRedis(cachedItems);
        }

        // Redis에 없으면 DB -> Redis 초기화
        log.info("[UserCart] Redis 캐시 미스 -> DB 조회 - userId={}", userId);
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            log.info("[UserCart] Cart 엔티티 미존재 - userId={}", userId);
            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0, 0, 0, 0, 0
            );
        }

        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        log.debug("[UserCart] DB cartItem 개수 - userId={}, count={}", userId, cartItems.size());

        // DB 결과를 Redis에 싱크(캐시 warm-up = 데이터 미리 채워넣기)
        syncUserCartCache(userId, cartItems);
        log.info("[UserCart] DB 결과를 Redis에 싱크 완료 - userId={}", userId);

        // 응답 반환 (DB 반환)
        return buildCartItemsResponse(cartItems);
    }

    // 2. 회원 장바구니 담기 – Redis만 수정 + dirty
    @Override
    public void addItemToUserCart(Long userId, CartItemRequestDto requestDto) {
        log.info("[UserCart] addItemToUserCart 호출 - userId={}, bookId={}, qty={}, selected={}",
                userId, requestDto.getBookId(), requestDto.getQuantity(), requestDto.isSelected());

        validateQuantity(requestDto.getQuantity());

        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        CartRedisItem item = userItems.get(requestDto.getBookId());
        boolean isNewItem = (item == null);

        if (isNewItem && (userItems.size() >= MAX_CART_SIZE)) {
            log.warn("[UserCart] 장바구니 최대 품목 수 초과 - userId={}, bookId={}, currentSize={}",
                    userId, requestDto.getBookId(), userItems.size());
            throw new CartBusinessException(
                    CartErrorCode.CART_SIZE_EXCEEDED,
                    "최대 " + MAX_CART_SIZE + "종의 상품만 담을 수 있습니다."
            );
        }

        int baseQuantity = (item != null) ? item.getQuantity() : 0;
        int requestedTotal = baseQuantity + requestDto.getQuantity();  // 기존 + 신규
        int capped = capQuantity(requestedTotal);  // 시스템 상한(예: 99) 적용

        log.debug("[UserCart] 수량 계산 - userId={}, bookId={}, baseQty={}, addQty={}, requestedTotal={}, capped={}",
                userId, requestDto.getBookId(), baseQuantity, requestDto.getQuantity(), requestedTotal, capped);

        BookSnapshot snapshot = requireBookSnapshot(requestDto.getBookId());
        validateBookAvailableForCart(snapshot);
        validateStockForQuantity(snapshot, capped);  // 실제로 저장할 수량 기준 검증

        CartRedisItem newItem;

        if (item != null) {
            item.setQuantity(capped);
            item.setSelected(requestDto.isSelected());
            item.touchUpdatedAt();
            newItem = item;

            log.debug("[UserCart] 기존 아이템 수량/선택 수정 - userId={}, bookId={}, newQty={}",
                    userId, requestDto.getBookId(), capped);
        } else {
            newItem = CartRedisItem.newItem(
                    requestDto.getBookId(),
                    capped,
                    requestDto.isSelected()
            );
            log.debug("[UserCart] 신규 아이템 추가 - userId={}, bookId={}, qty={}",
                    userId, requestDto.getBookId(), capped);
        }

        cartRedisRepository.putUserItem(userId, newItem);
        cartRedisRepository.markUserCartDirty(userId);

        log.info("[UserCart] addItemToUserCart 처리 완료 - userId={}, bookId={}, qty={}",
                userId, requestDto.getBookId(), capped);
    }

    // 3. 회원 장바구니 단건 상품 수량 변경
    @Override
    public void updateQuantityUserCartItem(Long userId, CartItemQuantityUpdateRequestDto requestDto) {
        log.info("[UserCart] updateQuantityUserCartItem 호출 - userId={}, bookId={}, newQty={}",
                userId, requestDto.getBookId(), requestDto.getQuantity());

        validateQuantity(requestDto.getQuantity());

        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        CartRedisItem item = userItems.get(requestDto.getBookId());
        if (item == null) {
            log.warn("[UserCart] 수량 변경 대상 cart item 없음 - userId={}, bookId={}", userId, requestDto.getBookId());
            throw new CartItemNotFoundException(
                    CartErrorCode.CART_ITEM_NOT_FOUND,
                    "Cart item을 찾을 수 없습니다. bookId=" + requestDto.getBookId()
            );
        }

        int capped = capQuantity(requestDto.getQuantity());

        BookSnapshot snapshot = requireBookSnapshot(requestDto.getBookId());
        validateBookAvailableForCart(snapshot);
        validateStockForQuantity(snapshot, capped);

        item.setQuantity(capped);
        item.touchUpdatedAt(); // 장바구니 항목이 수정될 때 갱신 시간을 기록

        cartRedisRepository.putUserItem(userId, item);
        cartRedisRepository.markUserCartDirty(userId);

        log.info("[UserCart] updateQuantityUserCartItem 완료 - userId={}, bookId={}, finalQty={}",
                userId, requestDto.getBookId(), capped);
    }

    // 4. 회원 장바구니 단건 상품 선택/해제
    @Override
    public void selectUserCartItem(Long userId, CartItemSelectRequestDto requestDto) {
        log.info("[UserCart] selectUserCartItem 호출 - userId={}, bookId={}, selected={}",
                userId, requestDto.getBookId(), requestDto.isSelected());

        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        CartRedisItem item = userItems.get(requestDto.getBookId());
        if (item == null) {
            throw new CartItemNotFoundException(
                    CartErrorCode.CART_ITEM_NOT_FOUND,
                    "Cart item을 찾을 수 없습니다. bookId=" + requestDto.getBookId()
            );
        }

        item.setSelected(requestDto.isSelected());
        item.touchUpdatedAt();
        cartRedisRepository.putUserItem(userId, item);
        cartRedisRepository.markUserCartDirty(userId);

        log.info("[UserCart] selectUserCartItem 완료 - userId={}, bookId={}, selected={}",
                userId, requestDto.getBookId(), requestDto.isSelected());
    }

    // 5. 회원 장바구니 상품 전체 선택/해제
    @Override
    public void selectAllUserCartItems(Long userId, CartItemSelectAllRequestDto requestDto) {
        log.info("[UserCart] selectAllUserCartItems 호출 - userId={}, selected={}", userId, requestDto.isSelected());

        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        if (userItems == null || userItems.isEmpty()) {
            log.info("[UserCart] 장바구니 비어있음 - userId={}", userId);
            return;
        }
        for (CartRedisItem item : userItems.values()) {
            item.setSelected(requestDto.isSelected());
            item.touchUpdatedAt(); // 시간 갱신
            cartRedisRepository.putUserItem(userId, item);
        }
        cartRedisRepository.markUserCartDirty(userId);

        log.info("[UserCart] selectAllUserCartItems 완료 - userId={}, itemCount={}",
                userId, userItems.size());
    }

    // 6. 회원 장바구니 단건 상품 제거
    @Override
    public void deleteUserCartItem(Long userId, CartItemDeleteRequestDto requestDto) {
        log.info("[UserCart] deleteUserCartItem 호출 - userId={}, bookId={}",
                userId, requestDto.getBookId());

        cartRedisRepository.deleteUserCartItem(userId, requestDto.getBookId());
        cartRedisRepository.markUserCartDirty(userId);

        log.info("[UserCart] deleteUserCartItem 완료 - userId={}, bookId={}",
                userId, requestDto.getBookId());
    }

    // 7. 회원 장바구니에서 선택된 모든 항목 제거
    @Override
    public void deleteSelectedUserCartItems(Long userId) {
        log.info("[UserCart] deleteSelectedUserCartItems 호출 - userId={}", userId);

        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        if (userItems == null || userItems.isEmpty()) {
            log.info("[UserCart] 장바구니 비어있음 - userId={}", userId);
            return;
        }
        for (CartRedisItem item : userItems.values()) {
            if (item.isSelected()) {
                cartRedisRepository.deleteUserCartItem(userId, item.getBookId());
            }
        }
        cartRedisRepository.markUserCartDirty(userId);
        log.info("[UserCart] deleteSelectedUserCartItems 완료 - userId={}", userId);
    }

    // 8. 회원 장바구니 선택된 항목 조회(주문)
    @Override
    @Transactional(readOnly = true)
    public CartItemsResponseDto getUserSelectedCart(Long userId) {
        log.info("[UserCart] getUserSelectedCart 호출 - userId={}", userId);

        // 1) 회원용 전체 장바구니 먼저 조회 (캐시/DB 로직 재사용)
        CartItemsResponseDto full = getUserCart(userId);
        // 2) 선택된 항목만 필터링해서 새 DTO 생성
        CartItemsResponseDto filtered = filterSelectedOnly(full);

        log.info("[UserCart] getUserSelectedCart 완료 - userId={}, selectedItemCount={}",
                userId, filtered.getItems().size());

        return filtered;
    }

    // 9. 회원 장바구니에 담긴 품목 개수 조회 (헤더 아이콘용)
    @Override
    @Transactional(readOnly = true)
    public CartItemCountResponseDto getUserCartCount(Long userId) {
        log.info("[UserCart] getUserCartCount 호출 - userId={}", userId);

        // 1) 캐시에서 먼저 개수 계산 (book-service 호출 없음)
        Map<Long, CartRedisItem> cachedItems = cartRedisRepository.getUserCartItems(userId);
        if (cachedItems != null && !cachedItems.isEmpty()) {
            // 캐시 기반으로 바로 응답 구성
            int itemCount = cachedItems.size();
            int totalQuantity = cachedItems.values().stream()
                    .mapToInt(CartRedisItem::getQuantity)
                    .sum();
            log.info("[UserCart] getUserCartCount 완료 (캐시 기반) - userId={}, itemCount={}, totalQuantity={}",
                    userId, itemCount, totalQuantity);
            return new CartItemCountResponseDto(itemCount, totalQuantity);
        }
        log.info("[UserCart] getUserCartCount 캐시 비어있음 -> 0 반환 - userId={}", userId);

        // DB fallback : 캐시가 비어 있으면 0,0 반환 (또는 DB 기준 계산 로직 추가 가능)
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return new CartItemCountResponseDto(0, 0);
        }

        List<CartItem> items = cartItemRepository.findByCart(cart);
        int itemCount = items.size();
        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

        // warm-up (다음부터 count도 캐시 기반 가능)
        syncUserCartCache(userId, items);

        return new CartItemCountResponseDto(itemCount, totalQuantity);
    }

    // 10. 회원 장바구니 전체 항목 비우기(결제 완료 시)
    // -> 주문 서비스와 연동하면서 “결제 성공 시 clearGuestCart 호출”로 연결
    @Override
    public void clearUserCart(Long userId) {
        log.info("[UserCart] clearUserCart 호출 - userId={}", userId);

        cartRedisRepository.clearUserCart(userId); // Redis에서 전체 삭제
        cartRedisRepository.markUserCartDirty(userId); // DB에서도 비워야 함

        log.info("[UserCart] clearUserCart 완료 - userId={}", userId);
    }


    // ==========================
    // 비회원(guest) 장바구니 (Redis)
    // ==========================
    // 1. 비회원 장바구니 전체 목록 조회
    @Override
    @Transactional(readOnly = true)
    public CartItemsResponseDto getGuestCart(String uuid) {
        log.info("[GuestCart] getGuestCart 호출 - uuid={}", uuid);

        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        log.debug("[GuestCart] getGuestCart Redis itemCount - uuid={}, count={}",
                uuid, redisItems != null ? redisItems.size() : 0);

        CartItemsResponseDto response = buildCartItemsResponseFromRedis(redisItems);
        log.info("[GuestCart] getGuestCart 완료 - uuid={}, itemCount={}",
                uuid, response.getItems().size());

        return response;
    }

    // 2. 비회원 장바구니 상품 추가 (수량 +1 또는 신규 추가)
    @Override
    public void addItemToGuestCart(String uuid, CartItemRequestDto requestDto) {
        log.info("[GuestCart] addItemToGuestCart 호출 - uuid={}, bookId={}, qty={}, selected={}",
                uuid, requestDto.getBookId(), requestDto.getQuantity(), requestDto.isSelected());

        validateQuantity(requestDto.getQuantity());

        // 1) Redis 데이터 조회
        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);

        CartRedisItem existing = redisItems.get(requestDto.getBookId());
        boolean isNewItem = (existing == null);

        // 2) 장바구니 사이즈 제한 검증 (신규 항목일 경우)
        if (isNewItem && redisItems.size() >= MAX_CART_SIZE) {
            log.warn("[GuestCart] 장바구니 최대 품목 수 초과 - uuid={}, currentSize={}, bookId={}",
                    uuid, redisItems.size(), requestDto.getBookId());
            throw new CartBusinessException(
                    CartErrorCode.CART_SIZE_EXCEEDED,
                    "최대 " + MAX_CART_SIZE + "종의 상품만 담을 수 있습니다."
            );
        }

        // 3) 수량 계산 및 상품 유효성 검증
        int baseQuantity = (existing != null) ? existing.getQuantity() : 0;
        int requestedTotal = baseQuantity + requestDto.getQuantity();
        int capped = capQuantity(requestedTotal);
        log.debug("[GuestCart] 수량 계산 - uuid={}, bookId={}, baseQty={}, addQty={}, requestedTotal={}, capped={}",
                uuid, requestDto.getBookId(), baseQuantity, requestDto.getQuantity(), requestedTotal, capped);

        // 4) 도서 스냅샷 조회 및 상태/재고 검증
        BookSnapshot snapshot = requireBookSnapshot(requestDto.getBookId());
        validateBookAvailableForCart(snapshot);
        validateStockForQuantity(snapshot, capped);

        CartRedisItem next;
        long now = System.currentTimeMillis();

        if (existing == null) {
            next = CartRedisItem.newItem(requestDto.getBookId(), capped, requestDto.isSelected());
        } else {
            existing.setQuantity(capped);
            existing.setSelected(requestDto.isSelected()); // selected 반영
            existing.setUpdatedAt(now);
            next = existing;
        }

        // 5) 실제 Redis 반영 (quantity+selected를 함께 저장하고 TTL도 갱신)
//        cartRedisRepository.updateGuestItemQuantity(uuid, requestDto.getBookId(), capped);
        cartRedisRepository.putGuestItem(uuid, next);

        log.info("[GuestCart] addItemToGuestCart 완료 - uuid={}, bookId={}, finalQty={}, selected={}",
                uuid, requestDto.getBookId(), capped, requestDto.isSelected());
    }

    // 3. 비회원 장바구니 내에서 단건 상품 수량 변경
    @Override
    public void updateQuantityGuestCartItem(String uuid, CartItemQuantityUpdateRequestDto requestDto) {
        log.info("[GuestCart] updateQuantityGuestCartItem 호출 - uuid={}, bookId={}, newQty={}",
                uuid, requestDto.getBookId(), requestDto.getQuantity());

        validateQuantity(requestDto.getQuantity());

        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        CartRedisItem existing = redisItems.get(requestDto.getBookId());

        if (existing == null) {
            log.warn("[GuestCart] 수량 변경 대상 cart item 없음 - uuid={}, bookId={}", uuid, requestDto.getBookId());
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

        log.info("[GuestCart] updateQuantityGuestCartItem 완료 - uuid={}, bookId={}, finalQty={}",
                uuid, requestDto.getBookId(), capped);
    }

    // 4. 비회원 장바구니 단건 상품 선택/해제
    @Override
    public void selectGuestCartItem(String uuid, CartItemSelectRequestDto requestDto) {
        log.info("[GuestCart] selectGuestCartItem 호출 - uuid={}, bookId={}, selected={}",
                uuid, requestDto.getBookId(), requestDto.isSelected());

        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        CartRedisItem item = redisItems.get(requestDto.getBookId());
        if (item == null) {
            log.warn("[GuestCart] 선택/해제 대상 cart item 없음 - uuid={}, bookId={}", uuid, requestDto.getBookId());
            throw new CartItemNotFoundException(
                    CartErrorCode.CART_ITEM_NOT_FOUND,
                    "비회원 장바구니의 아이템을 찾을 수 없습니다. bookId=" + requestDto.getBookId()
            );
        }

        item.setSelected(requestDto.isSelected());
        cartRedisRepository.putGuestItem(uuid, item);

        log.info("[GuestCart] selectGuestCartItem 완료 - uuid={}, bookId={}, selected={}",
                uuid, requestDto.getBookId(), requestDto.isSelected());
    }

    // 5. 비회원 장바구니 상품 전체 선택/해제
    @Override
    public void selectAllGuestCartItems(String uuid, CartItemSelectAllRequestDto requestDto) {
        log.info("[GuestCart] selectAllGuestCartItems 호출 - uuid={}, selected={}",
                uuid, requestDto.isSelected());

        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        if (redisItems == null || redisItems.isEmpty()) {
            log.info("[GuestCart] 장바구니 비어있음 - uuid={}", uuid);
            return;
        }

        boolean selected = requestDto.isSelected();

        for (CartRedisItem item : redisItems.values()) {
            item.setSelected(selected);
            cartRedisRepository.putGuestItem(uuid, item);
        }

        log.info("[GuestCart] selectAllGuestCartItems 완료 - uuid={}, itemCount={}",
                uuid, redisItems.size());
    }

    // 6. 비회원 장바구니 단건 상품 제거
    @Override
    public void deleteGuestCartItem(String uuid, CartItemDeleteRequestDto requestDto) {
        log.info("[GuestCart] deleteGuestCartItem 호출 - uuid={}, bookId={}",
                uuid, requestDto.getBookId());

        cartRedisRepository.deleteGuestItem(uuid, requestDto.getBookId());

        log.info("[GuestCart] deleteGuestCartItem 완료 - uuid={}, bookId={}",
                uuid, requestDto.getBookId());
    }

    // 7. 비회원 장바구니 선택된 항목 제거
    @Override
    public void deleteSelectedGuestCartItems(String uuid) {
        log.info("[GuestCart] deleteSelectedGuestCartItems 호출 - uuid={}", uuid);

        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);
        if (redisItems == null || redisItems.isEmpty()) {
            log.info("[GuestCart] 장바구니 비어있음 - uuid={}", uuid);
            return;
        }

        int removedCount = 0; // 로그 기록용
        for (CartRedisItem item : redisItems.values()) {
            if (item.isSelected()) {
                cartRedisRepository.deleteGuestItem(uuid, item.getBookId());
                removedCount++;
            }
        }
        log.info("[GuestCart] deleteSelectedGuestCartItems 완료 - uuid={}, removedCount={}",
                uuid, removedCount);
    }

    // 8. 비회원 장바구니 선택된 항목 조회(주문)
    @Override
    @Transactional(readOnly = true)
    public CartItemsResponseDto getGuestSelectedCart(String uuid) {
        log.info("[GuestCart] getGuestSelectedCart 호출 - uuid={}", uuid);

        CartItemsResponseDto full = getGuestCart(uuid);
        CartItemsResponseDto filtered = filterSelectedOnly(full);

        log.info("[GuestCart] getGuestSelectedCart 완료 - uuid={}, selectedItemCount={}",
                uuid, filtered.getItems().size());

        return filtered;
    }

    // 9. 비회원 장바구니의 품목 개수 조회
    @Override
    @Transactional(readOnly = true)
    public CartItemCountResponseDto getGuestCartCount(String uuid) {
        log.info("[GuestCart] getGuestCartCount 호출 - uuid={}", uuid);

        Map<Long, CartRedisItem> redisItems = cartRedisRepository.getGuestCartItems(uuid);

        if (redisItems == null || redisItems.isEmpty()) {
            log.info("[GuestCart] getGuestCartCount -> 비어있음, 0 반환 - uuid={}", uuid);
            return new CartItemCountResponseDto(0,0);
        }

        int itemCount = redisItems.size();
        int totalQuantity = redisItems.values().stream()
                .mapToInt(CartRedisItem::getQuantity)
                .sum();
        log.info("[GuestCart] getGuestCartCount 완료 - uuid={}, itemCount={}, totalQuantity={}",
                uuid, itemCount, totalQuantity);

        return new CartItemCountResponseDto(itemCount, totalQuantity);
    }

    // 10. 비회원 장바구니 전체 항목 비우기 (로그인 후 병합 완료 시)
    @Override
    public void clearGuestCart(String uuid) {
        log.info("[GuestCart] clearGuestCart 호출 - uuid={}", uuid);

        cartRedisRepository.clearGuestCart(uuid);

        log.info("[GuestCart] clearGuestCart 완료 - uuid={}", uuid);
    }


    // ==========================
    // guest(uuid) → user(DB) 병합(Merge)
    // ==========================

    // 1. 비회원 장바구니 데이터를 회원 장바구니(DB)로 병합 처리 (localStorage의 uuid + 로그인 후 userId)
    @Override
    public CartMergeResultResponseDto mergeGuestCartToUserCart(Long userId, String uuid) {
        log.info("[Merge] mergeGuestCartToUserCart 호출 - userId={}, uuid={}", userId, uuid);

        // 1) 비회원 장바구니 조회 및 예외 처리
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

        // 2) user cart는 Redis가 최신 권위. 캐시가 비어있다면 DB에서 warm-up 후 Redis로 기준을 맞춤.
        Map<Long, CartRedisItem> userItems = cartRedisRepository.getUserCartItems(userId);
        if (userItems == null || userItems.isEmpty()) {
            Cart cart = cartRepository.findByUserId(userId).orElse(null);
            if (cart != null) {
                List<CartItem> dbItems = cartItemRepository.findByCart(cart);
                syncUserCartCache(userId, dbItems);
                userItems = cartRedisRepository.getUserCartItems(userId);
            } else {
                userItems = Collections.emptyMap();
            }
        }

        int currentDistinctCount = userItems.size();

        log.debug("[Merge] 기존 user cart 상태 - userId={}, distinctCount={}", userId, currentDistinctCount);

        List<Long> guestBookIds = new ArrayList<>(guestItems.keySet());
        Map<Long, BookSnapshot> guestSnapshotMap = bookServiceClient.getBookSnapshots(guestBookIds);

        List<MergeIssueItemDto> exceededMaxQuantityItems = new ArrayList<>();
        List<MergeIssueItemDto> unavailableItems = new ArrayList<>();
        List<MergeIssueItemDto> failedToMergeItems = new ArrayList<>();

        // 3) Redis 기준으로 merge 결과를 user Redis cart에 직접 반영 + dirty 마킹
        for (Map.Entry<Long, CartRedisItem> entry : guestItems.entrySet()) {
            Long bookId = entry.getKey();
            CartRedisItem guestItem = entry.getValue();

            CartRedisItem userItem = userItems.get(bookId);
            int originalUserQuantity = (userItem != null) ? userItem.getQuantity() : 0;
            int guestQuantity = guestItem.getQuantity();

            boolean isNewItem = (userItem == null);
            if (isNewItem && currentDistinctCount >= MAX_CART_SIZE) {
                failedToMergeItems.add(new MergeIssueItemDto(
                        bookId, guestQuantity, originalUserQuantity, originalUserQuantity,
                        MergeIssueReason.CART_SIZE_LIMIT_EXCEEDED
                ));
                continue;
            }

            int requestedMergedQuantity = originalUserQuantity + guestQuantity;
            int finalMergedQuantity = Math.min(requestedMergedQuantity, MAX_QUANTITY);

            BookSnapshot snapshot = guestSnapshotMap.get(bookId);
            boolean isUnavailable = isBookUnavailable(snapshot);

            MergeIssueReason quantityIssueReason = null;
            if (snapshot != null && snapshot.getStockCount() > 0) {
                int stock = snapshot.getStockCount();
                if (finalMergedQuantity > stock) {
                    finalMergedQuantity = stock;
                    quantityIssueReason = MergeIssueReason.STOCK_LIMIT_EXCEEDED;
                } else if (requestedMergedQuantity > MAX_QUANTITY) {
                    quantityIssueReason = MergeIssueReason.EXCEEDED_MAX_QUANTITY;
                }
            } else {
                if (requestedMergedQuantity > MAX_QUANTITY) {
                    quantityIssueReason = MergeIssueReason.EXCEEDED_MAX_QUANTITY;
                }
            }
            if (quantityIssueReason != null) {
                exceededMaxQuantityItems.add(new MergeIssueItemDto(
                        bookId, guestQuantity, originalUserQuantity, finalMergedQuantity, quantityIssueReason
                ));
            }

            if (isUnavailable) {
                unavailableItems.add(new MergeIssueItemDto(
                        bookId, guestQuantity, originalUserQuantity, finalMergedQuantity, MergeIssueReason.UNAVAILABLE
                ));
            }

            boolean mergedSelected = true; // 정책: merge된 항목은 기본 선택 (원하면 userItem/guestItem selected OR로 바꿔도 됨)
            CartRedisItem merged = (userItem == null)
                    ? CartRedisItem.newItem(bookId, finalMergedQuantity, mergedSelected)
                    : userItem;

            merged.setQuantity(finalMergedQuantity);
            merged.setSelected(mergedSelected);
            merged.touchUpdatedAt();

            cartRedisRepository.putUserItem(userId, merged);
            cartRedisRepository.markUserCartDirty(userId);

            if (isNewItem) currentDistinctCount++;
        }

        // 5) guest cart 삭제
        cartRedisRepository.clearGuestCart(uuid);
        log.info("[Merge] guest cart 삭제 완료 - uuid={}", uuid);

        Map<Long, CartRedisItem> mergedUserItems = cartRedisRepository.getUserCartItems(userId);
        CartItemsResponseDto mergedDto = buildCartItemsResponseFromRedis(mergedUserItems);

        return new CartMergeResultResponseDto(
                mergedDto.getItems(),
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
        log.debug("[Util] filterSelectedOnly 호출");

        // 1) 초기 예외 처리 및 필터
        if (responseDto == null || responseDto.getItems() == null || responseDto.getItems().isEmpty()) {
            log.debug("[Util] filterSelectedOnly - 원본 items 비어있음");
            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        // 2) 선택 + 구매 가능(available=true) 아이템만 필터
        // -> 필터링을 통과한 selectedItems만을 기준으로 장바구니의 합계 금액을 새로 계산
        List<CartItemResponseDto> selectedItems = responseDto.getItems().stream()
                .filter(CartItemResponseDto::isSelected)
                .filter(CartItemResponseDto::isAvailable) // 품절/삭제/판매종료 제외
                .collect(Collectors.toList());
        if (selectedItems.isEmpty()) {
            log.debug("[Util] filterSelectedOnly - 선택 + 구매 가능 아이템 없음");
            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        // 3) 합계 다시 계산
        int totalItemCount = selectedItems.size();
        int totalQuantity = selectedItems.stream()
                .mapToInt(CartItemResponseDto::getQuantity)
                .sum();
        int totalPrice = selectedItems.stream()
                .mapToInt(item -> item.getSalePrice() * item.getQuantity())
                .sum();
        // selected 계열은 "전체=선택"으로 간주
        int selectedQuantity = totalQuantity;
        int selectedTotalPrice = totalPrice;
        log.debug("[Util] filterSelectedOnly 결과 - itemCount={}, totalQty={}, totalPrice={}",
                totalItemCount, totalQuantity, totalPrice);

        return new CartItemsResponseDto(
                selectedItems,
                totalItemCount,
                totalQuantity,
                totalPrice,
                selectedQuantity,
                selectedTotalPrice
        );
    }

    // 2. DB 결과를 Redis에 싱크
    private void syncUserCartCache(Long userId, List<CartItem> cartItems) {
        log.debug("[Util] syncUserCartCache 호출 - userId={}, itemCount={}",
                userId, cartItems != null ? cartItems.size() : 0);

        // 1) 기존 캐시 삭제 후, DB 값으로 다시 채워 넣기 (Cache Invalidation)
        cartRedisRepository.clearUserCart(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            log.debug("[Util] syncUserCartCache - 동기화할 데이터(cartItems == null) 없음 - userId={}", userId);
            return;
        }

        // 2) 조회 결과를 Redis 캐시에 적재
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
        log.debug("[Util] syncUserCartCache 완료 - userId={}, syncedItemCount={}",
                userId, cartItems.size());
    }

    // 3. 장바구니 엔티티 관리
    private Cart getOrCreateCart(Long userId) {
        log.debug("[Util] getOrCreateCart 호출 - userId={}", userId);

        // 해당 userId의 Cart 엔티티를 조회하거나, 없으면 새로 생성하여 반환
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.debug("[Util] Cart 엔티티 미존재 -> 신규 생성 - userId={}", userId);
                    Cart newCart = Cart.builder() // builder 사용은 “필수 필드만 세팅하고 나머지는 안정적으로 초기화”하기 위한 실무적 패턴
                            .userId(userId)
                            .build();
                    // 새로 생성된 Cart를 DB에 저장
                    log.debug("[Util] Cart 엔티티 저장 완료 - userId={}", userId);
                    return cartRepository.save(newCart);
                });
    }

    // 4. 유효성 검사 및 수량 처리
    // 4-1. 요청 수량이 최소/최대 허용 범위를 벗어나는지 검증
    private void validateQuantity(int quantity) {
        log.debug("[Util] validateQuantity 호출 - quantity={}", quantity);

        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            log.warn("[Util] 수량 범위 초과 - quantity={}, MIN={}, MAX={}",
                    quantity, MIN_QUANTITY, MAX_QUANTITY);
            throw new CartBusinessException(
                    CartErrorCode.INVALID_QUANTITY,
                    "수량은 " + MIN_QUANTITY + " 에서 " + MAX_QUANTITY + "사이여야 합니다. : " + quantity
            );
        }
    }

    // 4-2. 요청 수량을 MIN_QUANTITY와 MAX_QUANTITY 범위 내로 "강제" 조정하여 반환
    private int capQuantity(int quantity) {
        return Math.max(MIN_QUANTITY, Math.min(quantity, MAX_QUANTITY));
    }

    // 4-3. 도서 ID에 대한 최신 스냅샷(가격, 재고 등)을 조회하고 반환
    private BookSnapshot requireBookSnapshot(Long bookId) {
        try {
            Map<Long, BookSnapshot> map =
                    bookServiceClient.getBookSnapshots(Collections.singletonList(bookId));

            BookSnapshot snapshot = (map != null) ? map.get(bookId) : null;

            if (snapshot == null) {
                throw new CartBusinessException(
                        CartErrorCode.INVALID_BOOK_ID,
                        "유효하지 않은 도서입니다. bookId=" + bookId
                );
            }
            return snapshot;

        } catch (FeignException e) {
            // 외부 연동 장애는 invalid book과 분리
            throw new CartBusinessException(
                    CartErrorCode.BOOK_UNAVAILABLE, "도서 서비스 응답 오류로 도서 정보를 확인할 수 없습니다. bookId=" + bookId
            );
        } catch (Exception e) {
            throw new CartBusinessException(
                    CartErrorCode.BOOK_UNAVAILABLE, "도서 정보를 불러오는 중 내부 오류가 발생했습니다. bookId=" + bookId
            );
        }
    }

    // 4-4. 도서 스냅샷을 기반으로 현재 상품이 장바구니에 담을 수 있는 상태인지 검사
    private void validateBookAvailableForCart(BookSnapshot snapshot) {
        if (snapshot == null || isBookUnavailable(snapshot)) {
            log.warn("[Util] 구매 불가 상태 도서 - bookId={}",
                    snapshot != null ? snapshot.getBookId() : null);
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
            log.warn("[Util] 품절 도서 - bookId={}", snapshot.getBookId());
            throw new CartBusinessException(
                    CartErrorCode.OUT_OF_STOCK,
                    "품절된 도서입니다. 재고 = 0"
            );
        }
        if (requestedQuantity > stock) {
            log.warn("[Util] 재고 부족 - bookId={}, stock={}, requestedQty={}",
                    snapshot.getBookId(), stock, requestedQuantity);
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
        return snapshot.getStockCount() <= 0;
    }

    // ===== 응답 DTO(CartItemsResponseDto) 빌더 =====

    // 1. DB 결과 기반 응답 DTO
    // -> DB (CartItem) 결과를 기반으로 최신 스냅샷(가격, 재고) 정보를 포함한 최종 응답 DTO를 구성
    private CartItemsResponseDto buildCartItemsResponse(List<CartItem> items) {
        log.debug("[Builder] buildCartItemsResponse 호출 - itemCount={}",
                items != null ? items.size() : 0);
        // 1) 빈 장바구니 처리
        if (items == null || items.isEmpty()) {
            log.debug("[Builder] buildCartItemsResponse - 빈 장바구니");

            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0,      // totalItemCount
                    0,      // totalQuantity
                    0,      // totalPrice
                    0,      // selectedQuantity
                    0      // selectedTotalPrice
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

        log.debug("[Builder] buildCartItemsResponse - bookIds={}", bookIds);

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
                    pricing.getSalePrice(),
                    lineQuantity,
                    selected,
                    pricing.isAvailable(),
                    pricing.getUnavailableReason(),
                    pricing.getStockCount(),
                    pricing.isLowStock()
            );

            itemDtos.add(dto);
        }

        log.debug("[Builder] buildCartItemsResponse 결과 - totalItemCount={}, totalQuantity={}, totalPrice={}, selectedQuantity={}, selectedTotalPrice={}",
                totalItemCount, totalQuantity, totalPrice, selectedQuantity, selectedTotalPrice);

        // 7) 최종 CartItemsResponseDto 객체 생성 및 반환
        return new CartItemsResponseDto(
                itemDtos,
                totalItemCount,
                totalQuantity,
                totalPrice,
                selectedQuantity,
                selectedTotalPrice
        );
    }

    // 2. Redis 결과 기반 응답 DTO 구성
    // -> Redis (CartRedisItem) 캐시 결과를 기반으로 최신 스냅샷 정보를 포함한 최종 응답 DTO를 구성
    private CartItemsResponseDto buildCartItemsResponseFromRedis(Map<Long, CartRedisItem> redisItems) {

        if (redisItems == null || redisItems.isEmpty()) {
            log.debug("[Builder] buildCartItemsResponseFromRedis - 빈 장바구니");

            return new CartItemsResponseDto(
                    Collections.emptyList(),
                    0,      // totalItemCount
                    0,      // totalQuantity
                    0,      // totalPrice
                    0,      // selectedQuantity
                    0      // selectedTotalPrice
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
        log.debug("[Builder] buildCartItemsResponseFromRedis - bookIds={}", bookIds);

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
                    pricing.getSalePrice(),
                    lineQuantity,
                    selected,
                    pricing.isAvailable(),
                    pricing.getUnavailableReason(),
                    pricing.getStockCount(),
                    pricing.isLowStock()
            );

            itemDtos.add(dto);
        }

        log.debug("[Builder] buildCartItemsResponseFromRedis 결과 - totalItemCount={}, totalQuantity={}, totalPrice={}, selectedQuantity={}, selectedTotalPrice={}",
                totalItemCount, totalQuantity, totalPrice, selectedQuantity, selectedTotalPrice);

        return new CartItemsResponseDto(
                itemDtos,
                totalItemCount,
                totalQuantity,
                totalPrice,
                selectedQuantity,
                selectedTotalPrice
        );
    }
}
