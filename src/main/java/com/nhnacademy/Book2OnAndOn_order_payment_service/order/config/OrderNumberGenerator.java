package com.nhnacademy.Book2OnAndOn_order_payment_service.order.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {
    private final Snowflake snowflake;

    public String generate() {
        // Snowflake → 10자리 숫자로 변환
        long rawId = snowflake.nextId() % 1_000_000_0000L;
        String orderNumber = String.format("%010d", rawId);
        return "B2-" + orderNumber;
    }
}
