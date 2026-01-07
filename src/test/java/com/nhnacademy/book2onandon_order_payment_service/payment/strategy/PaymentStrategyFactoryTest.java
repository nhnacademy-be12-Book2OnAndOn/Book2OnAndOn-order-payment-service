package com.nhnacademy.book2onandon_order_payment_service.payment.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.book2onandon_order_payment_service.payment.exception.NotSupportedPayments;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentStrategyFactoryTest {

    private PaymentStrategyFactory factory;
    private PaymentStrategy tossStrategy;
    private PaymentStrategy naverStrategy;

    @BeforeEach
    void setUp() {
        tossStrategy = mock(PaymentStrategy.class);
        naverStrategy = mock(PaymentStrategy.class);

        // [수정] BDD 스타일인 given(...).willReturn(...)으로 통일
        given(tossStrategy.getProvider()).willReturn("TOSS");
        given(naverStrategy.getProvider()).willReturn("NAVER");

        factory = new PaymentStrategyFactory(List.of(tossStrategy, naverStrategy));
    }

    @Test
    @DisplayName("제공자 이름을 대문자로 요청해도 올바른 전략을 반환해야 한다")
    void getStrategy_UpperCase_Success() {
        PaymentStrategy result = factory.getStrategy("TOSS");

        assertThat(result).isEqualTo(tossStrategy);
    }

    @Test
    @DisplayName("제공자 이름을 소문자로 요청해도 대문자로 변환하여 올바른 전략을 반환해야 한다")
    void getStrategy_LowerCase_Success() {
        PaymentStrategy result = factory.getStrategy("toss");

        assertThat(result).isEqualTo(tossStrategy);
    }

    @Test
    @DisplayName("지원하지 않는 제공자를 요청하면 NotSupportedPayments 예외가 발생한다")
    void getStrategy_NotSupported_ThrowsException() {
        assertThatThrownBy(() -> factory.getStrategy("KAKAO"))
                .isExactlyInstanceOf(NotSupportedPayments.class)
                .hasMessageContaining("Not Supported Payments : KAKAO");
    }
}