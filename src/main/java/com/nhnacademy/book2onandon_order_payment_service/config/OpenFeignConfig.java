package com.nhnacademy.book2onandon_order_payment_service.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        // 개발 중에는 FULL, 운영에서는 BASIC 정도로 조정 (NONE, BASIC, HEADERS, FULL)
        return Logger.Level.FULL;
    }
}
