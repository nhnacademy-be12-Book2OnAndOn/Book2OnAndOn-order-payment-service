package com.nhnacademy.book2onandon_order_payment_service.config;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.CartRedisItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, CartRedisItem> cartRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        // RedisTemplate 인스턴스 생성 및 커넥션 설정
        RedisTemplate<String, CartRedisItem> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 사용되는 직렬화 방식 : StringRedisSerializer, Jackson2JsonRedisSerializer
        // key / hashKey
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        // hashKey: Long <-> String 변환
        GenericToStringSerializer<Long> hashKeySerializer = new GenericToStringSerializer<>(Long.class);

        // value / hashValue
        Jackson2JsonRedisSerializer<CartRedisItem> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, CartRedisItem.class);

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(hashKeySerializer); // Redis Hash 구조의 내부 필드 이름 직렬화

        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer); // Redis Hash 구조의 내부 필드 값 직렬화

        // 최종 초기화 및 빈 등록
        template.afterPropertiesSet();
        return template;
    }
}
