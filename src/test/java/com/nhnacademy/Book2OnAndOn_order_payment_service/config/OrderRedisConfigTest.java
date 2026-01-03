package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

class OrderRedisConfigTest {

    private final OrderRedisConfig orderRedisConfig = new OrderRedisConfig();

    @Test
    @DisplayName("RedisTemplate이 String 시리얼라이저와 함께 정상적으로 생성된다")
    void orderNumberRedisTemplate_Success() {
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);

        RedisTemplate<String, String> template = orderRedisConfig.orderNumberRedisTemplate(mockFactory);

        assertThat(template).isNotNull();
        assertThat(template.getConnectionFactory()).isEqualTo(mockFactory);
        
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    @DisplayName("설정 클래스의 인스턴스가 생성되는지 확인한다")
    void configInstanceTest() {
        assertThat(orderRedisConfig).isNotNull();
    }
}