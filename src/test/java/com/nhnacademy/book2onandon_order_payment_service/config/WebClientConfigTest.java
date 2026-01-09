package com.nhnacademy.book2onandon_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

class WebClientConfigTest {

    @Test
    @DisplayName("WebClient Bean이 정상적으로 생성되고 등록되어야 한다")
    void webClientBeanCreationTest() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebClientConfig.class);

        WebClient webClient = context.getBean(WebClient.class);

        assertThat(webClient).isNotNull();
        context.close();
    }

    @Test
    @DisplayName("설정 클래스의 메서드를 직접 호출하여 WebClient 인스턴스를 검증한다")
    void webClientInstanceTest() {
        WebClientConfig config = new WebClientConfig();
        
        WebClient webClient = config.webClient();

        assertThat(webClient)
                .isNotNull()
                .isInstanceOf(WebClient.class);
    }
}