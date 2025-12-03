package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemDeleteRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemQuantityUpdateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectAllRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.request.CartItemSelectRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemCountResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartItemsResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.CartMergeResultResponseDto;

public interface CartService {

    // ==========================
    // 회원 장바구니
    // ==========================
    // 회원의 장바구니 전체 목록 조회 (가격, 재고 스냅샷 포함)
    CartItemsResponseDto getUserCart(Long userId);

    // 회원의 장바구니에 상품 추가 (수량 +1 또는 신규 추가)
    void addItemToUserCart(Long userId, CartItemRequestDto requestDto);

    // 회원의 장바구니 특정 상품 수량 변경
    void updateUserItemQuantity(Long userId, CartItemQuantityUpdateRequestDto requestDto);

    // 회원의 장바구니에서 특정 상품 제거
    void removeItemFromUserCart(Long userId, CartItemDeleteRequestDto requestDto);

    // 회원의 장바구니 특정 상품 선택/해제 상태 변경
    void selectUserCartItem(Long userId, CartItemSelectRequestDto requestDto);

    // 회원의 장바구니 상품 전체 선택/해제 상태 변경
    void selectAllUserCartItems(Long userId, CartItemSelectAllRequestDto requestDto);

    // 회원의 장바구니에 담긴 서로 다른 상품 개수 조회 (헤더 아이콘용)
    CartItemCountResponseDto getUserCartCount(Long userId);

    // 회원의 장바구니 전체 항목 비우기
    void clearUserCart(Long userId);

    // 회원의 장바구니에서 선택된 모든 항목 제거
    void deleteSelectedUserCartItem(Long userId);

    // 회원 장바구니 선택된 항목 조회(주문)
    CartItemsResponseDto getUserSelectedCart(Long userId);

    // ==========================
    // 비회원(guest) 장바구니
    // ==========================
    // 비회원의 장바구니 전체 목록 조회 (가격, 재고 스냅샷 포함)
    CartItemsResponseDto getGuestCart(String uuid);

    // 비회원의 장바구니에 상품 추가 (수량 +1 또는 신규 추가)
    void addItemToGuestCart(String uuid, CartItemRequestDto requestDto);

    // 비회원의 장바구니 특정 상품 수량 변경
    void updateGuestItemQuantity(String uuid, CartItemQuantityUpdateRequestDto requestDto);

    // 비회원의 장바구니에서 특정 상품 제거
    void removeItemFromGuestCart(String uuid, CartItemDeleteRequestDto requestDto);

    // 비회원의 장바구니 특정 상품 선택/해제 상태 변경
    void selectGuestCartItem(String uuid, CartItemSelectRequestDto requestDto);

    // 비회원의 장바구니 상품 전체 선택/해제 상태 변경
    void selectAllGuestCartItems(String uuid, CartItemSelectAllRequestDto requestDto);

    // 비회원의 장바구니에 담긴 서로 다른 상품 개수 조회 (헤더 아이콘용)
    CartItemCountResponseDto getGuestCartCount(String uuid);

    // 비회원의 장바구니 전체 항목 비우기 (예: 로그인 후 병합 완료 시)
    void clearGuestCart(String uuid);

    // 비회원의 장바구니에서 선택된 모든 항목 제거
    void deleteSelectedGuestCartItem(String uuid);

    // 비회원 장바구니 선택된 항목 조회(주문)
    CartItemsResponseDto getGuestSelectedCart(String uuid);

    // ==========================
    // guest → user 병합
    // ==========================
    // 비회원 장바구니 데이터를 회원 장바구니(DB)로 병합 처리
    CartMergeResultResponseDto mergeGuestCartToUserCart(Long userId, String uuid);
}