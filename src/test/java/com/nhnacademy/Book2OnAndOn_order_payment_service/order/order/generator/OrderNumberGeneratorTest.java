package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.Book2OnAndOn_order_payment_service.config.Snowflake;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.generator.OrderNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderNumberGeneratorTest {

    private Snowflake snowflake;
    private OrderNumberGenerator generator;

    @BeforeEach
    void setUp() {
        snowflake = mock(Snowflake.class);
        generator = new OrderNumberGenerator(snowflake);
    }

    @Test
    @DisplayName("Snowflake ID를 기반으로 'B2-' 접두사가 붙은 12자리 주문 번호를 생성한다")
    void generateOrderNumber_Success() {
        long mockId = 123456789L;
        given(snowflake.nextId()).willReturn(mockId);

        String result = generator.generate();

        assertThat(result).startsWith("B2-");
        assertThat(result).hasSize(15);
        assertThat(result).isEqualTo("B2-000123456789");
    }

    @Test
    @DisplayName("12자리를 초과하는 Snowflake ID가 들어와도 하위 12자리만 사용하여 생성한다")
    void generateOrderNumber_TruncateOverTwelveDigits() {
        long largeId = 1_000_000_000_001L;
        given(snowflake.nextId()).willReturn(largeId);

        String result = generator.generate();

        assertThat(result).isEqualTo("B2-000000000001");
    }

    @Test
    @DisplayName("생성된 주문 번호는 항상 동일한 포맷(B2-000000000000)을 유지해야 한다")
    void generateOrderNumber_FormatConsistency() {
        given(snowflake.nextId()).willReturn(0L);

        String result = generator.generate();

        assertThat(result).matches("^B2-\\d{12}$");
    }
}