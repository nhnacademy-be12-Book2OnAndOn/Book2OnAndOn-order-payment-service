package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CartErrorCode {

    // 공통
    INVALID_QUANTITY("수량이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    CART_SIZE_EXCEEDED("장바구니에 담을 수 있는 상품 종류 수를 초과했습니다.", HttpStatus.BAD_REQUEST),
    INVALID_BOOK_ID("유효하지 않은 도서 ID입니다.", HttpStatus.BAD_REQUEST),

    // 상품/재고
    OUT_OF_STOCK("재고가 부족합니다.", HttpStatus.BAD_REQUEST),
    BOOK_UNAVAILABLE( "구매할 수 없는 상태의 도서입니다.", HttpStatus.BAD_REQUEST),

    // 장바구니/아이템
    CART_NOT_FOUND("장바구니를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND( "장바구니 아이템을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // merge 관련
    MERGE_FAILED("장바구니 병합 과정에서 오류가 발생했습니다.", HttpStatus.CONFLICT),

    // 동시성 관련
    CONCURRENCY_CONFLICT("장바구니가 동시에 수정되어 충돌이 발생했습니다.", HttpStatus.CONFLICT);

    private final String message;
    private final HttpStatus httpStatus;

    CartErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.httpStatus = status;
    }
}
