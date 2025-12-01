package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemQuantityUpdateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectAllRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartMergeRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartMergeResultResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartUserController {

    private static final String USER_ID_HEADER = "X-USER-ID";

    private final CartService cartService;

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
    public ResponseEntity<Void> updateUserItemQuantity(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartItemQuantityUpdateRequestDto requestDto
    ) {
        cartService.updateUserItemQuantity(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 4. 회원 장바구니 단일 아이템 삭제
    // DELETE /cart/items/{bookId}
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> removeItemFromUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long bookId
    ) {
        CartItemDeleteRequestDto dto = new CartItemDeleteRequestDto(bookId);
        cartService.removeItemFromUserCart(userId, dto);
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
        cartService.deleteSelectedUserCartItem(userId);
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
    // GET /cart/count
    @GetMapping("/count")
    public ResponseEntity<CartItemCountResponseDto> getUserCartCount(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        CartItemCountResponseDto cartItemCount = cartService.getUserCartCount(userId);
        return ResponseEntity.ok().body(cartItemCount);
    }

    // 10. 회원 장바구니 중 "선택된 + 구매 가능한" 항목만 조회 (주문용)
    @GetMapping("/selected")
    public ResponseEntity<CartItemsResponseDto> getUserSelectedCart(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        CartItemsResponseDto selectedCart = cartService.getUserSelectedCart(userId);
        return ResponseEntity.ok(selectedCart);
    }

    // 11. 비회원 → 회원 장바구니 병합
    // POST /cart/merge
    // Body: { "guestUuid": "uuid-xxxx" }
    @PostMapping("/merge")
    public ResponseEntity<CartMergeResultResponseDto> mergeGuestCartToUserCart(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CartMergeRequestDto requestDto
    ) {
        CartMergeResultResponseDto cartMergeResult = cartService.mergeGuestCartToUserCart(userId, requestDto);
        return ResponseEntity.ok().body(cartMergeResult);
    }
}
