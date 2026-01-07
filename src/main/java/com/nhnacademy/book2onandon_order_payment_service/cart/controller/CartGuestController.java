package com.nhnacademy.book2onandon_order_payment_service.cart.controller;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemQuantityUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemSelectAllRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemSelectRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartErrorCode;
import com.nhnacademy.book2onandon_order_payment_service.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart/guest")
@RequiredArgsConstructor
public class CartGuestController {

    private static final String GUEST_ID_HEADER = "X-Guest-Id";

    private final CartService cartService;

    private void validateUuid(String uuid) {
        if (uuid.isBlank()) {
            throw new CartBusinessException(CartErrorCode.GUEST_ID_REQUIRED);
        }
    }

    private void validateBookId(Long bookId) {
        // PathVariable이 Long 변환 자체에 실패하면(예: not-a-number) 여기까지 오지 못하지만,
        // bookId가 0 이하 같은 “논리적으로 유효하지 않은 값”은 여기서 통일 처리.
        if (bookId <= 0) {
            throw new CartBusinessException(CartErrorCode.INVALID_BOOK_ID);
        }
    }

    // 1. 비회원 장바구니 조회
    // GET /cart/guest
    @GetMapping
    public ResponseEntity<CartItemsResponseDto> getGuestCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        validateUuid(uuid);
        CartItemsResponseDto guestCart = cartService.getGuestCart(uuid);
        return ResponseEntity.ok().body(guestCart);
    }

    // 2. 비회원 장바구니 추가
    // POST /cart/guest/items
    @PostMapping("/items")
    public ResponseEntity<Void> addItemToGuestCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @Valid @RequestBody CartItemRequestDto requestDto
    ) {
        validateUuid(uuid);
        cartService.addItemToGuestCart(uuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 3. 비회원 장바구니 수량 변경
    // PATCH /cart/guest/items/quantity
    @PatchMapping("/items/quantity")
    public ResponseEntity<Void> updateQuantityGuestCartItem(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @Valid @RequestBody CartItemQuantityUpdateRequestDto requestDto
    ) {
        validateUuid(uuid);
        cartService.updateQuantityGuestCartItem(uuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 4. 비회원 장바구니 단건 아이템 선택/해제
    // PATCH /cart/guest/items/select
    @PatchMapping("/items/select")
    public ResponseEntity<Void> selectGuestCartItem(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @Valid @RequestBody CartItemSelectRequestDto requestDto
    ) {
        validateUuid(uuid);
        cartService.selectGuestCartItem(uuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 5. 비회원 장바구니 전체 선택/해제
    // PATCH /cart/guest/items/select-all
    @PatchMapping("/items/select-all")
    public ResponseEntity<Void> selectAllGuestCartItems(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @Valid @RequestBody CartItemSelectAllRequestDto requestDto
    ) {
        validateUuid(uuid);
        cartService.selectAllGuestCartItems(uuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 6. 비회원 장바구니 단건 아이템 삭제
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> deleteGuestCartItem(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @PathVariable Long bookId
    ) {
        validateUuid(uuid);
        validateBookId(bookId);

        CartItemDeleteRequestDto dto = new CartItemDeleteRequestDto(bookId);
        cartService.deleteGuestCartItem(uuid, dto);
        return ResponseEntity.ok().build();
    }

    // 7. 비회원 장바구니 "선택된" 항목 삭제
    // DELETE /cart/guest/items/selected
    @DeleteMapping("/items/selected")
    public ResponseEntity<Void> deleteSelectedGuestCartItems(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        validateUuid(uuid);
        cartService.deleteSelectedGuestCartItems(uuid);
        return ResponseEntity.ok().build();
    }

    // 8. 비회원 장바구니 중 "선택된 + 구매 가능한" 항목만 조회 (주문용)
    @GetMapping("/items/selected")
    public ResponseEntity<CartItemsResponseDto> getGuestSelectedCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        validateUuid(uuid);
        CartItemsResponseDto selectedCart = cartService.getGuestSelectedCart(uuid);
        return ResponseEntity.ok(selectedCart);
    }

    // 9. 아이콘용 장바구니 개수 조회 (비회원)
    @GetMapping("/items/count")
    public ResponseEntity<CartItemCountResponseDto> getGuestCartCount(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        validateUuid(uuid);
        CartItemCountResponseDto guestCartCount = cartService.getGuestCartCount(uuid);
        return ResponseEntity.ok().body(guestCartCount);
    }

    // 10. 비회원 장바구니 전체 항목 비우기
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearGuestCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        validateUuid(uuid);
        cartService.clearGuestCart(uuid);
        return ResponseEntity.ok().build();
    }
}
