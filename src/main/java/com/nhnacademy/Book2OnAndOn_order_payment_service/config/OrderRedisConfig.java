package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class OrderRedisConfig {
//    @Bean(name = "temporaryOrderRedisTemplate")
//    public RedisTemplate<String, TempOrder> temporaryOrderRedisTemplate(
//            RedisConnectionFactory connectionFactory,
//            ObjectMapper objectMapper
//    ) {
//        RedisTemplate<String, TempOrder> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        // Key Serializer : String (PREFIX + orderNumber)
//        StringRedisSerializer keySerializer =
//                new StringRedisSerializer();
//
//        template.setKeySerializer(keySerializer);
//        template.setHashKeySerializer(keySerializer);
//
//        // Value Serializer : JSON (Temporary Order)
//        Jackson2JsonRedisSerializer<TempOrder> valueSerializer =
//                new Jackson2JsonRedisSerializer<>(objectMapper, TempOrder.class);
//
//        template.setValueSerializer(valueSerializer);
//        template.setHashValueSerializer(valueSerializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }

    @Bean(name = "orderNumberRedisTemplate")
    public RedisTemplate<String, String> orderNumberRedisTemplate(
            RedisConnectionFactory connectionFactory
    ){
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);

        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);

        template.afterPropertiesSet();

        return template;
    }
}
