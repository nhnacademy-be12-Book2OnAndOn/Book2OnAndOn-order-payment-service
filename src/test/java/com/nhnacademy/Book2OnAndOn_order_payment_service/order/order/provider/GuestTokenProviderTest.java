package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.GuestTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestTokenProviderTest {

    private GuestTokenProvider guestTokenProvider;
    private final String secretKey = "test-secret-key-must-be-at-least-32-characters-long";

    @BeforeEach
    void setUp() {
        guestTokenProvider = new GuestTokenProvider(secretKey);
    }

    @Test
    @DisplayName("주문 아이디를 포함한 유효한 JWT 토큰을 생성한다")
    void createToken_Success() {
        Long orderId = 123L;

        String token = guestTokenProvider.createToken(orderId);

        assertThat(token).isNotBlank();
        assertThat(guestTokenProvider.validateTokenAndGetOrderId(token)).isEqualTo(orderId);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이나 변조된 토큰 검증 시 null을 반환한다")
    void validateToken_InvalidToken_ReturnsNull() {
        String invalidToken = "invalid.token.value";

        Long result = guestTokenProvider.validateTokenAndGetOrderId(invalidToken);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("다른 비밀키로 생성된 토큰 검증 시 null을 반환한다")
    void validateToken_WrongKey_ReturnsNull() {
        Long orderId = 123L;
        GuestTokenProvider anotherProvider = new GuestTokenProvider("another-secret-key-for-testing-purposes");
        String tokenFromAnother = anotherProvider.createToken(orderId);

        Long result = guestTokenProvider.validateTokenAndGetOrderId(tokenFromAnother);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("토큰에서 주문 아이디를 정상적으로 추출한다")
    void validateTokenAndGetOrderId_Success() {
        Long orderId = 999L;
        String token = guestTokenProvider.createToken(orderId);

        Long extractedOrderId = guestTokenProvider.validateTokenAndGetOrderId(token);

        assertThat(extractedOrderId).isEqualTo(orderId);
    }
}