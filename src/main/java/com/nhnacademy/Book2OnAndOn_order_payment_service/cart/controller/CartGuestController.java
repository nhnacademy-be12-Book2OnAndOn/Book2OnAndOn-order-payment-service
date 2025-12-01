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

    private final CartService cartService;

    // 1. 비회원 장바구니 조회
    // GET /cart/guest?guestUuid=xxx
    @GetMapping
    public ResponseEntity<CartItemsResponseDto> getGuestCart(@RequestParam("guestUuid") String guestUuid) {
        CartItemsResponseDto guestCart = cartService.getGuestCart(guestUuid);
        return ResponseEntity.ok().body(guestCart);
    }

    // 2. 비회원 장바구니 담기
    // POST /cart/guest/items?guestUuid=xxx
    @PostMapping("/items")
    public ResponseEntity<Void> addItemToGuestCart(
            @RequestParam("guestUuid") String guestUuid,
            @Valid @RequestBody CartItemRequestDto requestDto
    ) {
        cartService.addItemToGuestCart(guestUuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 3. 비회원 장바구니 수량 변경
    // PATCH /cart/guest/items/quantity?guestUuid=xxx
    @PatchMapping("/items/quantity")
    public ResponseEntity<Void> updateGuestItemQuantity(
            @RequestParam("guestUuid") String guestUuid,
            @Valid @RequestBody CartItemQuantityUpdateRequestDto requestDto
    ) {
        cartService.updateGuestItemQuantity(guestUuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 4. 비회원 장바구니 단일 아이템 삭제
    // DELETE /cart/guest/items/{bookId}?guestUuid=xxx
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> removeItemFromGuestCart(
            @RequestParam("guestUuid") String guestUuid,
            @PathVariable Long bookId
    ) {
        CartItemDeleteRequestDto dto = new CartItemDeleteRequestDto(bookId);
        cartService.removeItemFromGuestCart(guestUuid, dto);
        return ResponseEntity.ok().build();
    }

    // 5. 비회원 장바구니 전체 항목 삭제
    // DELETE /cart/guest/items?guestUuid=xxx
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearGuestCart(@RequestParam("guestUuid") String guestUuid) {
        cartService.clearGuestCart(guestUuid);
        return ResponseEntity.ok().build();
    }

    // 6. 비회원 장바구니 "선택된" 항목 삭제
    // DELETE /cart/guest/items/selected?guestUuid=xxx
    @DeleteMapping("/items/selected")
    public ResponseEntity<Void> deleteSelectedGuestCartItems(
            @RequestParam("guestUuid") String guestUuid
    ) {
        cartService.deleteSelectedGuestCartItem(guestUuid);
        return ResponseEntity.ok().build();
    }

    // 7. 비회원 장바구니 단건 선택/해제
    // PATCH /cart/guest/items/select?guestUuid=xxx
    @PatchMapping("/items/select")
    public ResponseEntity<Void> selectGuestCartItem(
            @RequestParam("guestUuid") String guestUuid,
            @Valid @RequestBody CartItemSelectRequestDto requestDto
    ) {
        cartService.selectGuestCartItem(guestUuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 8. 비회원 장바구니 전체 선택/해제
    // PATCH /cart/guest/items/select-all?guestUuid=xxx
    @PatchMapping("/items/select-all")
    public ResponseEntity<Void> selectAllGuestCartItems(
            @RequestParam("guestUuid") String guestUuid,
            @Valid @RequestBody CartItemSelectAllRequestDto requestDto
    ) {
        cartService.selectAllGuestCartItems(guestUuid, requestDto);
        return ResponseEntity.ok().build();
    }

    // 9. 아이콘용 장바구니 개수 조회 (비회원)
    // GET /api/cart/guest/count?guestUuid=xxx
    @GetMapping("/count")
    public ResponseEntity<CartItemCountResponseDto> getGuestCartCount(@RequestParam("guestUuid") String guestUuid) {
        CartItemCountResponseDto guestCartCount = cartService.getGuestCartCount(guestUuid);
        return ResponseEntity.ok().body(guestCartCount);
    }

    // 10. 비회원 장바구니 중 "선택된 + 구매 가능한" 항목만 조회 (주문용)
    @GetMapping("/selected")
    public ResponseEntity<CartItemsResponseDto> getGuestSelectedCart(
            @RequestParam("guestUuid") String guestUuid
    ) {
        CartItemsResponseDto selectedCart = cartService.getGuestSelectedCart(guestUuid);
        return ResponseEntity.ok(selectedCart);
    }
}
