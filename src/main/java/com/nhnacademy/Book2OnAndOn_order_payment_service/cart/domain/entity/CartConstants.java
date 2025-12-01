package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity;

public final class CartConstants {

    private CartConstants() {}

    // 장바구니 수량 제한
    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 99;

    // 장바구니 아이템의 종류 제한
    public static final int MAX_CART_SIZE = 99;

    // 재고 부족 표시 임계값
    public static final int LOW_STOCK_THRESHOLD = 10;

    // Redis guest cart TTL(시간 단위)
    public static final long GUEST_CART_TTL_HOURS = 24L;

    // Redis 키 prefix
    public static final String USER_CART_KEY_PREFIX = "cart:user:";
    public static final String GUEST_CART_KEY_PREFIX  = "cart:guest:";

    // 회원 장바구니 데이터는 일반적인 Key-Value 방식이 아니라, 하나의 Key 안에 여러 필드를 갖는 Hash 형태로 저장됩니다.
    // - 메인 Key (장바구니 ID): userKey(userId) (예: cart:user:123 = 사용자 ID 123의 장바구니 전체) -> 엔티티_종류 : 식별자 : 필드
    // - 필드 Key (상품 ID): bookId (예: 1, 2, 3...)
    // - 필드 Value (장바구니 항목 정보): CartRedisItem 객체 (수량, 선택 여부)

}

