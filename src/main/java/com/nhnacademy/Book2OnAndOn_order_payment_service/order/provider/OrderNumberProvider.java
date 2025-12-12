package com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNumberProvisionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderNumberProvider {

    private final RedisTemplate<String, String> orderNumberRedisTemplate;
    private static final String ORDER_NUMBER_QUEUE_KEY = "order-service:order-number:queue";

    @Transactional
    public String provideOrderNumber(){
        try{
            return orderNumberRedisTemplate.opsForList().rightPop(ORDER_NUMBER_QUEUE_KEY);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 연결 실패", e);
            throw new OrderNumberProvisionException("주문 번호 발급 오류 발생", e);
        } catch (SerializationException e) {
            log.error("Redis 직렬화 실패", e);
            throw new OrderNumberProvisionException("주문 번호 발급 오류 발생", e);
        } catch (Exception e) {
            log.error("알 수 없는 오류", e);
            throw new OrderNumberProvisionException("주문 번호 발급 오류 발생", e);
        }
    }
}
