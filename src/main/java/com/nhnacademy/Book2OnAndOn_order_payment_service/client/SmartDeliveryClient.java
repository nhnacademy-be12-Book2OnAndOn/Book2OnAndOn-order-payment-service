package com.nhnacademy.Book2OnAndOn_order_payment_service.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.SmartTrackingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmartDeliveryClient {

    private final WebClient webClient;

//    @Value("${smart-delivery.api-key}")
    private String apiKey; // application.properties에 설정 필요

    /**
     * 배송 완료 여부 확인
     * @return true(완료), false(미완료 또는 실패)
     */
    public boolean isDeliveryCompleted(String companyCode, String waybill) {
        try {
            // WebClient 호출 (동기 처리: block())
            // 스케줄러는 순차적으로 처리해야 하므로 block()을 써도 안전
            SmartTrackingResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/trackingInfo")
                            .queryParam("t_key", apiKey)
                            .queryParam("t_code", companyCode)
                            .queryParam("t_invoice", waybill)
                            .build())
                    .retrieve()
                    .bodyToMono(SmartTrackingResponse.class)
                    .block(); // 결과가 올 때까지 기다림

            if (response == null) {
                return false;
            }

            // 완료 조건: complete가 true이거나 level이 6인 경우
            return Boolean.TRUE.equals(response.getComplete()) ||
                    (response.getLevel() != null && response.getLevel() == 6);

        } catch (Exception e) {
            log.error("스마트택배 조회 실패 (운송장: {}): {}", waybill, e.getMessage());
            return false;
        }
    }
}