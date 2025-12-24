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

    // Redis guest cart TTL
    // 1) 비회원 장바구니를 48시간마다 갱신(활동 기반)하는 정책 (비활성 기준, 시간 단위)
    public static final long GUEST_CART_TTL_HOURS = 48L;
    // 2) 비회원 장바구니 유지를 최대 7일을 넘기지 않는 만료 정책 (생성 시점 기준, 일 단위)
    public static final long GUEST_CART_MAX_LIFETIME_DAYS = 7L;

    // Redis 키 prefix
    public static final String USER_CART_KEY_PREFIX = "cart-service:user:";
    public static final String GUEST_CART_KEY_PREFIX  = "cart-service:guest:";

    // Dirty set key (회원 장바구니 write-behind용)
    public static final String USER_CART_DIRTY_SET_KEY = "cart-service:user:dirty";

    // 회원 장바구니 데이터는 일반적인 Key-Value 방식이 아니라, 하나의 Key 안에 여러 필드를 갖는 "Hash 형태"로 저장됩니다.
    // - 메인 Key (장바구니 ID): userKey(userId) (예: cart:user:123 = 사용자 ID 123의 장바구니 전체) -> 엔티티_종류 : 식별자 : 필드
    // - 필드 Key (상품 ID): bookId (예: 1, 2, 3...)
    // - 필드 Value (장바구니 항목 정보): CartRedisItem 객체 (수량, 선택 여부)

}

