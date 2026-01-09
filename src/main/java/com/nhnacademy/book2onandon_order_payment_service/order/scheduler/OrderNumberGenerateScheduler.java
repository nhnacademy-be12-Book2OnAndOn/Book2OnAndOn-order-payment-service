package com.nhnacademy.book2onandon_order_payment_service.order.scheduler;

import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNumberGenerateException;
import com.nhnacademy.book2onandon_order_payment_service.order.generator.OrderNumberGenerator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderNumberGenerateScheduler {

    private final RedisTemplate<String, String> orderNumberRedisTemplate;
    private final OrderNumberGenerator orderNumberGenerator;

    private static final String ORDER_NUMBER_SCHEDULER_ERROR_MSG = "주문 번호 생성 스케줄러 오류 발생";

    private static final String ORDER_NUMBER_QUEUE_KEY = "order-service:order-number:queue";
    private static final int ORDER_NUMBER_QUEUE_SIZE = 500;

    // 1분마다 추가
    @Scheduled(fixedRate = 60_000)
    public void fillOrderNumberQueue(){
        try {
            Long size = orderNumberRedisTemplate.opsForList().size(ORDER_NUMBER_QUEUE_KEY);

            // Redis 큐에 저장된 주문 번호의 개수가 500개 미만일시 500개까지 채움
            while (Objects.isNull(size) || size < ORDER_NUMBER_QUEUE_SIZE) {
                log.info("주문 번호 생성 스케줄러 시작 (남은 주문번호 개수 : {})", size);
                String newOrderNumber = orderNumberGenerator.generate();
                orderNumberRedisTemplate.opsForList().leftPush(ORDER_NUMBER_QUEUE_KEY, newOrderNumber);
                log.info("주문 번호 추가 완료");
                size = orderNumberRedisTemplate.opsForList().size(ORDER_NUMBER_QUEUE_KEY);
            }
        } catch (RedisConnectionFailureException e) {
            log.error(ORDER_NUMBER_SCHEDULER_ERROR_MSG, e);
            throw new OrderNumberGenerateException("Redis 연결 실패", e);
        } catch (SerializationException e) {
            log.error(ORDER_NUMBER_SCHEDULER_ERROR_MSG, e);
            throw new OrderNumberGenerateException("Redis 직렬화 실패", e);
        } catch (Exception e) {
            log.error(ORDER_NUMBER_SCHEDULER_ERROR_MSG, e);
            throw new OrderNumberGenerateException("알 수 없는 오류", e);
        }
    }
}
