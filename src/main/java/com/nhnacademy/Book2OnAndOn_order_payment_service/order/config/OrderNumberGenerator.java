package com.nhnacademy.Book2OnAndOn_order_payment_service.order.config;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final Snowflake snowflake;
    private final OrderRepository orderRepository;

    public String generate() {
        while (true) {
            // Snowflake → 10자리 숫자로 변환
            long rawId = snowflake.nextId() % 1_000_000_0000L;
            String orderNumber = String.format("%010d", rawId);

            orderNumber = "B2-" + orderNumber;

            // 유니크 제약으로 충돌 검사
            if(orderRepository.existsByOrderNumber(orderNumber)){
                // 충돌 발생 시 재생성
                continue;
            }

            return orderNumber;
        }
    }
}
