package com.nhnacademy.book2onandon_order_payment_service.cart.controller;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemQuantityUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemSelectAllRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemSelectRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartMergeResultResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartMergeStatusResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartRedisItem;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartErrorCode;
import com.nhnacademy.book2onandon_order_payment_service.cart.repository.CartRedisRepository;
import com.nhnacademy.book2onandon_order_payment_service.cart.service.CartService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart/user")
@RequiredArgsConstructor
public class CartUserController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String GUEST_ID_HEADER = "X-Guest-Id";

    private final CartService cartService;
    private final CartRedisRepository cartRedisRepository;

    private void validateUserId(Long userId) {
        if (userId <= 0) {
            throw new CartBusinessException(CartErrorCode.INVALID_USER_ID);
        }
    }

    private void validateBookId(Long bookId) {
        if (bookId <= 0) {
            throw new CartBusinessException(CartErrorCode.INVALID_BOOK_ID);
        }
    }

    private void validateGuestUuidRequired(String uuid) {
        // merge 처럼 guestId가 필수인 API에서 사용
        if (uuid.isBlank()) {
            throw new CartBusinessException(CartErrorCode.GUEST_ID_REQUIRED);
        }
    }

    private void validateGuestUuidOptional(String uuid) {
        // merge-status 처럼 guestId가 선택인 API에서 사용
        // null은 허용, blank는 잘못된 입력으로 400
        if (uuid != null && uuid.isBlank()) {
            throw new CartBusinessException(CartErrorCode.GUEST_ID_REQUIRED);
        }
    }

    // 1. 회원 장바구니 조회
    @GetMapping
    public ResponseEntity<CartItemsResponseDto> getUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        validateUserId(userId);
        CartItemsResponseDto userCart = cartService.getUserCart(userId);
        return ResponseEntity.ok().body(userCart);
    }

    // 2. 회원 장바구니 담기
    @PostMapping("/items")
    public ResponseEntity<Void> addItemToUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemRequestDto requestDto
    ) {
        validateUserId(userId);
        cartService.addItemToUserCart(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 3. 회원 장바구니 수량 변경
    @PatchMapping("/items/quantity")
    public ResponseEntity<Void> updateQuantityUserCartItem(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemQuantityUpdateRequestDto requestDto
    ) {
        validateUserId(userId);
        cartService.updateQuantityUserCartItem(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 4. 회원 장바구니 단일 아이템 삭제
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> deleteUserCartItem(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long bookId
    ) {
        validateUserId(userId);
        validateBookId(bookId);

        CartItemDeleteRequestDto dto = new CartItemDeleteRequestDto(bookId);
        cartService.deleteUserCartItem(userId, dto);
        return ResponseEntity.ok().build();
    }

    // 5. 회원 장바구니 전체 항목 삭제
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        validateUserId(userId);
        cartService.clearUserCart(userId);
        return ResponseEntity.ok().build();
    }

    // 6. 회원 장바구니 "선택된" 항목 삭제
    @DeleteMapping("/items/selected")
    public ResponseEntity<Void> deleteSelectedUserCartItems(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        validateUserId(userId);
        cartService.deleteSelectedUserCartItems(userId);
        return ResponseEntity.ok().build();
    }

    // 7. 회원 장바구니 단건 선택/해제
    @PatchMapping("/items/select")
    public ResponseEntity<Void> selectUserCartItem(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemSelectRequestDto requestDto
    ) {
        validateUserId(userId);
        cartService.selectUserCartItem(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 8. 회원 장바구니 전체 선택/해제
    @PatchMapping("/items/select-all")
    public ResponseEntity<Void> selectAllUserCartItems(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemSelectAllRequestDto requestDto
    ) {
        validateUserId(userId);
        cartService.selectAllUserCartItems(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 9. 아이콘용 장바구니 개수 조회 (회원)
    @GetMapping("/items/count")
    public ResponseEntity<CartItemCountResponseDto> getUserCartCount(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        validateUserId(userId);
        CartItemCountResponseDto cartItemCount = cartService.getUserCartCount(userId);
        return ResponseEntity.ok().body(cartItemCount);
    }

    // 10. 회원 장바구니 중 "선택된 + 구매 가능한" 항목만 조회 (주문용)
    @GetMapping("/items/selected")
    public ResponseEntity<CartItemsResponseDto> getUserSelectedCart(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        validateUserId(userId);
        CartItemsResponseDto selectedCart = cartService.getUserSelectedCart(userId);
        return ResponseEntity.ok(selectedCart);
    }

    // 11. 비회원 → 회원 장바구니 병합
    @PostMapping("/merge")
    public ResponseEntity<CartMergeResultResponseDto> mergeGuestCartToUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        validateUserId(userId);
        validateGuestUuidRequired(uuid);

        CartMergeResultResponseDto cartMergeResult = cartService.mergeGuestCartToUserCart(userId, uuid);
        return ResponseEntity.ok().body(cartMergeResult);
    }

    // 12. 머지 체크용
    @GetMapping("/merge-status")
    public ResponseEntity<CartMergeStatusResponseDto> getMergeStatus(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestHeader(value = GUEST_ID_HEADER, required = false) String uuid
    ) {
        validateUserId(userId);
        validateGuestUuidOptional(uuid);

        Map<Long, CartRedisItem> guestItems =
                (uuid == null) ? Map.of() : cartRedisRepository.getGuestCartItems(uuid);

        boolean hasGuestCart = guestItems != null && !guestItems.isEmpty();

        CartItemsResponseDto userCart = cartService.getUserCart(userId);
        boolean hasUserCart = userCart != null && userCart.getTotalItemCount() > 0;

        int guestItemCount = hasGuestCart ? guestItems.size() : 0;

        return ResponseEntity.ok(
                new CartMergeStatusResponseDto(
                        hasGuestCart,
                        hasUserCart,
                        guestItemCount
                )
        );
    }
}
