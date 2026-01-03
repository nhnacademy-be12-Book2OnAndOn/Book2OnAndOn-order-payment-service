package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotSupportedPayments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NaverPaymentStrategyTest {

    private NaverPaymentStrategy naverPaymentStrategy;

    @BeforeEach
    void setUp() {
        naverPaymentStrategy = new NaverPaymentStrategy();
    }

    @Test
    @DisplayName("제공자 이름이 NAVER여야 한다")
    void getProviderTest() {
        assertThat(naverPaymentStrategy.getProvider()).isEqualTo("NAVER");
    }

    @Test
    @DisplayName("결제 승인 요청 시 NotSupportedPayments 예외를 던져야 한다")
    void confirmPaymentTest() {
        CommonConfirmRequest req = mock(CommonConfirmRequest.class);
        String idempotencyKey = "test-key";

        assertThatThrownBy(() -> naverPaymentStrategy.confirmPayment(req, idempotencyKey))
                .isExactlyInstanceOf(NotSupportedPayments.class)
                .hasMessageContaining("현재 지원하지 않는 결제 서비스 입니다");
    }

    @Test
    @DisplayName("결제 취소 요청 시 NotSupportedPayments 예외를 던져야 한다")
    void cancelPaymentTest() {
        CommonCancelRequest req = mock(CommonCancelRequest.class);
        String idempotencyKey = "test-key";

        assertThatThrownBy(() -> naverPaymentStrategy.cancelPayment(req, idempotencyKey))
                .isExactlyInstanceOf(NotSupportedPayments.class)
                .hasMessageContaining("현재 지원하지 않는 결제 서비스 입니다");
    }
}