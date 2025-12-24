package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemQuantityUpdateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectAllRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartMergeResultResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartMergeStatusResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartRedisItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.repository.CartRedisRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.service.CartService;
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

    // 1. 회원 장바구니 조회
    // GET /cart
    @GetMapping
    public ResponseEntity<CartItemsResponseDto> getUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        CartItemsResponseDto userCart = cartService.getUserCart(userId);
        return ResponseEntity.ok().body(userCart);
    }

    // 2. 회원 장바구니 담기
    // POST /cart/items
    @PostMapping("/items")
    public ResponseEntity<Void> addItemToUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemRequestDto requestDto
    ) {
        cartService.addItemToUserCart(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 3. 회원 장바구니 수량 변경 (절대값 변경; +1/-1는 프론트에서 계산 후 quantity로 전달)
    // PATCH /cart/items/quantity
    @PatchMapping("/items/quantity")
    public ResponseEntity<Void> updateQuantityUserCartItem(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemQuantityUpdateRequestDto requestDto
    ) {
        cartService.updateQuantityUserCartItem(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 4. 회원 장바구니 단일 아이템 삭제
    // DELETE /cart/items/{bookId}
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> deleteUserCartItem(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long bookId
    ) {
        CartItemDeleteRequestDto dto = new CartItemDeleteRequestDto(bookId);
        cartService.deleteUserCartItem(userId, dto);
        return ResponseEntity.ok().build();
    }

    // 5. 회원 장바구니 전체 항목 삭제
    // DELETE /cart/items
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        cartService.clearUserCart(userId);
        return ResponseEntity.ok().build();
    }

    // 6. 회원 장바구니 "선택된" 항목 삭제
    // DELETE /cart/items/selected
    @DeleteMapping("/items/selected")
    public ResponseEntity<Void> deleteSelectedUserCartItems(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        cartService.deleteSelectedUserCartItems(userId);
        return ResponseEntity.ok().build();
    }

    // 7. 회원 장바구니 단건 선택/해제
    // PATCH /cart/items/select
    @PatchMapping("/items/select")
    public ResponseEntity<Void> selectUserCartItem(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemSelectRequestDto requestDto
    ) {
        cartService.selectUserCartItem(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 8. 회원 장바구니 전체 선택/해제
    // PATCH /cart/items/select-all
    @PatchMapping("/items/select-all")
    public ResponseEntity<Void> selectAllUserCartItems(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemSelectAllRequestDto requestDto
    ) {
        cartService.selectAllUserCartItems(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 9. 아이콘용 장바구니 개수 조회 (회원)
    // GET /cart/items/count
    @GetMapping("/items/count")
    public ResponseEntity<CartItemCountResponseDto> getUserCartCount(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        CartItemCountResponseDto cartItemCount = cartService.getUserCartCount(userId);
        return ResponseEntity.ok().body(cartItemCount);
    }

    // 10. 회원 장바구니 중 "선택된 + 구매 가능한" 항목만 조회 (주문용)
    @GetMapping("/items/selected")
    public ResponseEntity<CartItemsResponseDto> getUserSelectedCart(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        CartItemsResponseDto selectedCart = cartService.getUserSelectedCart(userId);
        return ResponseEntity.ok(selectedCart);
    }

    // 11. 비회원 → 회원 장바구니 병합
    // POST /cart/merge
    // Body: { "uuid": "uuid-xxxx" }
    @PostMapping("/merge")
    public ResponseEntity<CartMergeResultResponseDto> mergeGuestCartToUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        CartMergeResultResponseDto cartMergeResult = cartService.mergeGuestCartToUserCart(userId, uuid);
        return ResponseEntity.ok().body(cartMergeResult);
    }

    // 12. 머지 체크용
    // -> 로그인한 사용자가 장바구니 페이지에 들어왔을 때 머지 여부를 물어보는 모달 생성용
    @GetMapping("/merge-status")
    public ResponseEntity<CartMergeStatusResponseDto> getMergeStatus(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestHeader(value = GUEST_ID_HEADER, required = false) String uuid
    ) {
        // 1) guest cart 존재 여부
//        Map<Long, CartRedisItem> guestItems = cartRedisRepository.getGuestCartItems(uuid);
        Map<Long, CartRedisItem> guestItems = (uuid == null) ? Map.of() : cartRedisRepository.getGuestCartItems(uuid);
        boolean hasGuestCart = guestItems != null && !guestItems.isEmpty();

        // 2) user cart 존재 여부 (DB or Redis)
        CartItemsResponseDto userCart = cartService.getUserCart(userId);
        boolean hasUserCart = userCart.getTotalItemCount() > 0;

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
