package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // 1. HttpClient 설정 (타임아웃 등)
        HttpClient httpClient = HttpClient.create()
                // 연결 타임아웃 (Connect Timeout) - 5초
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                // 응답 타임아웃 (Response Timeout) - 5초 (전체 응답 대기 시간)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))  // 읽기 타임아웃 (패킷 간 간격)
                            .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS))); // 쓰기 타임아웃

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}