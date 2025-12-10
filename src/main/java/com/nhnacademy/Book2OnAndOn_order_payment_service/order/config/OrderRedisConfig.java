package com.nhnacademy.Book2OnAndOn_order_payment_service.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.TemporaryOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class OrderRedisConfig {
    @Bean(name = "temporaryOrderRedisTemplate")
    public RedisTemplate<String, TemporaryOrder> temporaryOrderRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, TemporaryOrder> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer : String (PREFIX + orderNumber)
        StringRedisSerializer keySerializer =
                new StringRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // Value Serializer : JSON (Temporary Order)
        Jackson2JsonRedisSerializer<TemporaryOrder> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, TemporaryOrder.class);

        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
