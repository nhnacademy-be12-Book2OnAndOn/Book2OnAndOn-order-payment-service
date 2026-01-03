package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.entity.CartRedisItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    @DisplayName("장바구니 RedisTemplate이 각 필드에 맞는 시리얼라이저와 함께 정상 생성된다")
    void cartRedisTemplate_Success() {
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);
        ObjectMapper mockMapper = mock(ObjectMapper.class);

        RedisTemplate<String, CartRedisItem> template = redisConfig.cartRedisTemplate(mockFactory, mockMapper);

        assertThat(template).isNotNull();
        assertThat(template.getConnectionFactory()).isEqualTo(mockFactory);

        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);

        assertThat(template.getHashKeySerializer()).isInstanceOf(GenericToStringSerializer.class);

        assertThat(template.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
    }

    @Test
    @DisplayName("RedisConfig 인스턴스가 정상적으로 생성된다")
    void configInstanceTest() {
        assertThat(redisConfig).isNotNull();
    }
}