package com.nhnacademy.book2onandon_order_payment_service.order.scheduler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNumberGenerateException;
import com.nhnacademy.book2onandon_order_payment_service.order.generator.OrderNumberGenerator;
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
class OrderNumberGenerateSchedulerTest {

    @Mock
    private RedisTemplate<String, String> orderNumberRedisTemplate;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    @InjectMocks
    private OrderNumberGenerateScheduler orderNumberGenerateScheduler;

    @Test
    @DisplayName("Redis 큐가 부족할 경우 설정된 사이즈까지 주문 번호를 생성하여 충전한다")
    void fillOrderNumberQueue_Success() {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        given(orderNumberRedisTemplate.opsForList()).willReturn(listOps);
        
        given(listOps.size(anyString()))
                .willReturn(498L)
                .willReturn(499L)
                .willReturn(500L);
        given(orderNumberGenerator.generate()).willReturn("ORD-1", "ORD-2");

        orderNumberGenerateScheduler.fillOrderNumberQueue();

        verify(orderNumberGenerator, times(2)).generate();
        verify(listOps, times(2)).leftPush(anyString(), anyString());
    }

    @Test
    @DisplayName("Redis 연결 실패 시 OrderNumberGenerateException이 발생한다")
    void fillOrderNumberQueue_RedisConnectionFailure() {
        given(orderNumberRedisTemplate.opsForList()).willThrow(new RedisConnectionFailureException("Connection Refused"));

        assertThatThrownBy(() -> orderNumberGenerateScheduler.fillOrderNumberQueue())
                .isInstanceOf(OrderNumberGenerateException.class)
                .hasMessageContaining("Redis 연결 실패");
    }

    @Test
    @DisplayName("직렬화 오류 발생 시 OrderNumberGenerateException이 발생한다")
    void fillOrderNumberQueue_SerializationFailure() {
        given(orderNumberRedisTemplate.opsForList()).willThrow(new SerializationException("Serial Error"));

        assertThatThrownBy(() -> orderNumberGenerateScheduler.fillOrderNumberQueue())
                .isInstanceOf(OrderNumberGenerateException.class)
                .hasMessageContaining("Redis 직렬화 실패");
    }

    @Test
    @DisplayName("예기치 못한 오류 발생 시 OrderNumberGenerateException으로 래핑하여 던진다")
    void fillOrderNumberQueue_UnknownException() {
        given(orderNumberRedisTemplate.opsForList()).willThrow(new RuntimeException("Unknown Error"));

        assertThatThrownBy(() -> orderNumberGenerateScheduler.fillOrderNumberQueue())
                .isInstanceOf(OrderNumberGenerateException.class)
                .hasMessageContaining("알 수 없는 오류");
    }

    @Test
    @DisplayName("Redis 큐가 이미 가득 차 있다면 번호를 생성하지 않는다")
    void fillOrderNumberQueue_AlreadyFull() {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        given(orderNumberRedisTemplate.opsForList()).willReturn(listOps);
        given(listOps.size(anyString())).willReturn(500L);

        orderNumberGenerateScheduler.fillOrderNumberQueue();

        verify(orderNumberGenerator, times(0)).generate();
    }
}