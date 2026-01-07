package com.nhnacademy.book2onandon_order_payment_service.cart.service;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemQuantityUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemSelectAllRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request.CartItemSelectRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.response.CartMergeResultResponseDto;
import java.util.List;

public interface CartService {

    // ==========================
    // 회원 장바구니
    // ==========================
    // 1) 회원의 장바구니 전체 목록 조회 (가격, 재고 스냅샷 포함)
    CartItemsResponseDto getUserCart(Long userId);

    // 2) 회원의 장바구니에 상품 추가 (수량 +1 또는 신규 추가)
    void addItemToUserCart(Long userId, CartItemRequestDto requestDto);

    // 3) 회원의 장바구니 단건 상품 수량 변경
    void updateQuantityUserCartItem(Long userId, CartItemQuantityUpdateRequestDto requestDto);

    // 4) 회원의 장바구니 단건 상품 선택/해제
    void selectUserCartItem(Long userId, CartItemSelectRequestDto requestDto);

    // 5) 회원의 장바구니 상품 전체 선택/해제
    void selectAllUserCartItems(Long userId, CartItemSelectAllRequestDto requestDto);

    // 6) 회원의 장바구니에서 단건 상품 제거
    void deleteUserCartItem(Long userId, CartItemDeleteRequestDto requestDto);

    // 7) 회원의 장바구니에서 선택된 모든 항목 제거
    void deleteSelectedUserCartItems(Long userId);

    void deleteUserCartItemsAfterPayment(Long userId, List<Long> purchasedBookIds);

    // 8) 회원 장바구니 선택된 항목 조회(주문)
    CartItemsResponseDto getUserSelectedCart(Long userId);

    // 9) 회원의 장바구니에 담긴 서로 다른 상품 개수 조회 (헤더 아이콘용)
    CartItemCountResponseDto getUserCartCount(Long userId);

    // 10) 회원의 장바구니 전체 항목 비우기
    void clearUserCart(Long userId);

    // ==========================
    // 비회원(guest) 장바구니
    // ==========================
    // 1) 비회원의 장바구니 전체 목록 조회 (가격, 재고 스냅샷 포함)
    CartItemsResponseDto getGuestCart(String uuid);

    // 2) 비회원의 장바구니에 상품 추가 (수량 +1 또는 신규 추가)
    void addItemToGuestCart(String uuid, CartItemRequestDto requestDto);

    // 3) 비회원의 장바구니 단건 상품 수량 변경
    void updateQuantityGuestCartItem(String uuid, CartItemQuantityUpdateRequestDto requestDto);

    // 4) 비회원의 장바구니 단건 상품 선택/해제
    void selectGuestCartItem(String uuid, CartItemSelectRequestDto requestDto);

    // 5) 비회원의 장바구니 상품 전체 선택/해제
    void selectAllGuestCartItems(String uuid, CartItemSelectAllRequestDto requestDto);

    // 6) 비회원의 장바구니에서 단건 상품 제거
    void deleteGuestCartItem(String uuid, CartItemDeleteRequestDto requestDto);

    // 7) 비회원의 장바구니에서 선택된 항목 제거
    void deleteSelectedGuestCartItems(String uuid);

    // 8) 비회원 장바구니 선택된 항목 조회(주문)
    CartItemsResponseDto getGuestSelectedCart(String uuid);

    // 9) 비회원의 장바구니에 담긴 서로 다른 상품 개수 조회 (헤더 아이콘용)
    CartItemCountResponseDto getGuestCartCount(String uuid);

    // 10) 비회원의 장바구니 전체 항목 비우기 (전체 삭제, 병합 완료, 주문 완료 등...)
    void clearGuestCart(String uuid);


    // ==========================
    // guest → user 병합
    // ==========================
    // 1) 비회원 장바구니 데이터를 회원 장바구니(DB)로 병합 처리
    CartMergeResultResponseDto mergeGuestCartToUserCart(Long userId, String uuid);
}