package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemQuantityUpdateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectAllRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.service.CartService;
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

    // 1. 비회원 장바구니 조회
    // GET /cart/guest?uuid=xxx
    @GetMapping
    public ResponseEntity<CartItemsResponseDto> getGuestCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        CartItemsResponseDto guestCart = cartService.getGuestCart(uuid);
        return ResponseEntity.ok().body(guestCart);
    }

    // 2. 비회원 장바구니 담기
    // POST /cart/guest/items?uuid=xxx
    @PostMapping("/items")
    public ResponseEntity<Void> addItemToGuestCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @Valid @RequestBody CartItemRequestDto requestDto
    ) {
        cartService.addItemToGuestCart(uuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 3. 비회원 장바구니 수량 변경
    // PATCH /cart/guest/items/quantity?uuid=xxx
    @PatchMapping("/items/quantity")
    public ResponseEntity<Void> updateGuestItemQuantity(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @Valid @RequestBody CartItemQuantityUpdateRequestDto requestDto
    ) {
        cartService.updateGuestItemQuantity(uuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 4. 비회원 장바구니 단일 아이템 삭제
    // DELETE /cart/guest/items/{bookId}?uuid=xxx
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> removeItemFromGuestCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @PathVariable Long bookId
    ) {
        CartItemDeleteRequestDto dto = new CartItemDeleteRequestDto(bookId);
        cartService.removeItemFromGuestCart(uuid, dto);
        return ResponseEntity.ok().build();
    }

    // 5. 비회원 장바구니 전체 항목 삭제
    // DELETE /cart/guest/items?uuid=xxx
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearGuestCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        cartService.clearGuestCart(uuid);
        return ResponseEntity.ok().build();
    }

    // 6. 비회원 장바구니 "선택된" 항목 삭제
    // DELETE /cart/guest/items/selected?uuid=xxx
    @DeleteMapping("/items/selected")
    public ResponseEntity<Void> deleteSelectedGuestCartItems(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        cartService.deleteSelectedGuestCartItem(uuid);
        return ResponseEntity.ok().build();
    }

    // 7. 비회원 장바구니 단건 선택/해제
    // PATCH /cart/guest/items/select?uuid=xxx
    @PatchMapping("/items/select")
    public ResponseEntity<Void> selectGuestCartItem(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @Valid @RequestBody CartItemSelectRequestDto requestDto
    ) {
        cartService.selectGuestCartItem(uuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 8. 비회원 장바구니 전체 선택/해제
    // PATCH /cart/guest/items/select-all?uuid=xxx
    @PatchMapping("/items/select-all")
    public ResponseEntity<Void> selectAllGuestCartItems(
            @RequestHeader(GUEST_ID_HEADER) String uuid,
            @Valid @RequestBody CartItemSelectAllRequestDto requestDto
    ) {
        cartService.selectAllGuestCartItems(uuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 9. 아이콘용 장바구니 개수 조회 (비회원)
    // GET /api/cart/guest/count?uuid=xxx
    @GetMapping("/items/count")
    public ResponseEntity<CartItemCountResponseDto> getGuestCartCount(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        CartItemCountResponseDto guestCartCount = cartService.getGuestCartCount(uuid);
        return ResponseEntity.ok().body(guestCartCount);
    }

    // 10. 비회원 장바구니 중 "선택된 + 구매 가능한" 항목만 조회 (주문용)
    @GetMapping("/items/selected")
    public ResponseEntity<CartItemsResponseDto> getGuestSelectedCart(
            @RequestHeader(GUEST_ID_HEADER) String uuid
    ) {
        CartItemsResponseDto selectedCart = cartService.getGuestSelectedCart(uuid);
        return ResponseEntity.ok(selectedCart);
    }
}
