package com.nhnacademy.book2onandon_order_payment_service.order.order.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNumberProvisionException;
import com.nhnacademy.book2onandon_order_payment_service.order.provider.OrderNumberProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;

@ExtendWith(MockitoExtension.class)
class OrderNumberProviderTest {

    @Mock
    private RedisTemplate<String, String> orderNumberRedisTemplate;

    @InjectMocks
    private OrderNumberProvider orderNumberProvider;

    @Test
    @DisplayName("Redis 큐에서 주문 번호를 정상적으로 발급받는다")
    void provideOrderNumber_Success() {
        String expectedOrderNumber = "ORD-20251229-001";
        ListOperations<String, String> listOps = mock(ListOperations.class);

        given(orderNumberRedisTemplate.opsForList()).willReturn(listOps);
        given(listOps.rightPop(anyString())).willReturn(expectedOrderNumber);

        String result = orderNumberProvider.provideOrderNumber();

        assertThat(result).isEqualTo(expectedOrderNumber);
    }

    @Test
    @DisplayName("Redis 연결 실패 시 OrderNumberProvisionException이 발생한다")
    void provideOrderNumber_ConnectionFailure() {
        given(orderNumberRedisTemplate.opsForList()).willThrow(new RedisConnectionFailureException("Conn error"));

        assertThatThrownBy(() -> orderNumberProvider.provideOrderNumber())
                .isInstanceOf(OrderNumberProvisionException.class)
                .hasCauseInstanceOf(RedisConnectionFailureException.class);
    }

    @Test
    @DisplayName("Redis 데이터 직렬화 오류 시 OrderNumberProvisionException이 발생한다")
    void provideOrderNumber_SerializationFailure() {
        given(orderNumberRedisTemplate.opsForList()).willThrow(new SerializationException("Serial error"));

        assertThatThrownBy(() -> orderNumberProvider.provideOrderNumber())
                .isInstanceOf(OrderNumberProvisionException.class)
                .hasCauseInstanceOf(SerializationException.class);
    }

    @Test
    @DisplayName("알 수 없는 일반 예외 발생 시 OrderNumberProvisionException이 발생한다")
    void provideOrderNumber_UnknownFailure() {
        given(orderNumberRedisTemplate.opsForList()).willThrow(new RuntimeException("Unknown error"));

        assertThatThrownBy(() -> orderNumberProvider.provideOrderNumber())
                .isInstanceOf(OrderNumberProvisionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}