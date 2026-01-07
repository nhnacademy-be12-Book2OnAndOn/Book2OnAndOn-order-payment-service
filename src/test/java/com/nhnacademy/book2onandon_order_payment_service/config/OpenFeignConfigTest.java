package com.nhnacademy.book2onandon_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class OpenFeignConfigTest {

    @Test
    @DisplayName("Feign 로깅 레벨이 FULL로 설정된 Bean이 정상적으로 생성되어야 한다")
    void feignLoggerLevelBeanTest() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(OpenFeignConfig.class);

        Logger.Level level = context.getBean(Logger.Level.class);

        assertThat(level).isEqualTo(Logger.Level.FULL);
        context.close();
    }

    @Test
    @DisplayName("설정 클래스 자체의 인스턴스가 생성되는지 확인한다")
    void configInstanceTest() {
        OpenFeignConfig config = new OpenFeignConfig();
        
        Logger.Level level = config.feignLoggerLevel();

        assertThat(config).isNotNull();
        assertThat(level).isEqualTo(Logger.Level.FULL);
    }
}