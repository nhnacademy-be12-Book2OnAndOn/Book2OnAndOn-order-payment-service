package com.nhnacademy.book2onandon_order_payment_service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nhnacademy.book2onandon_order_payment_service.client.dto.SmartTrackingResponse;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class SmartDeliveryClientTest {

    @Mock
    private WebClient webClient;

    @InjectMocks
    private SmartDeliveryClient smartDeliveryClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smartDeliveryClient, "apiKey", "test-api-key");
    }

    @Test
    @DisplayName("배송 완료 정보(complete=true)를 받으면 true를 반환한다")
    void isDeliveryCompleted_CompleteTrue_ReturnsTrue() {
        // [수정] 기본 생성자 호출 후 리플렉션으로 필드 주입
        SmartTrackingResponse response = new SmartTrackingResponse();
        ReflectionTestUtils.setField(response, "complete", true);
        ReflectionTestUtils.setField(response, "level", 5);

        setupWebClientMock(response);

        boolean result = smartDeliveryClient.isDeliveryCompleted("04", "123456");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("배송 단계가 6단계(배송완료)인 정보를 받으면 true를 반환한다")
    void isDeliveryCompleted_LevelSix_ReturnsTrue() {
        SmartTrackingResponse response = new SmartTrackingResponse();
        ReflectionTestUtils.setField(response, "complete", false);
        ReflectionTestUtils.setField(response, "level", 6);

        setupWebClientMock(response);

        boolean result = smartDeliveryClient.isDeliveryCompleted("04", "123456");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("배송이 완료되지 않았거나 응답이 없으면 false를 반환한다")
    void isDeliveryCompleted_NotCompleted_ReturnsFalse() {
        SmartTrackingResponse response = new SmartTrackingResponse();
        ReflectionTestUtils.setField(response, "complete", false);
        ReflectionTestUtils.setField(response, "level", 3);

        setupWebClientMock(response);

        boolean result = smartDeliveryClient.isDeliveryCompleted("04", "123456");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("API 호출 중 예외가 발생하면 false를 반환한다")
    void isDeliveryCompleted_Exception_ReturnsFalse() {
        given(webClient.get()).willThrow(new RuntimeException("API Error"));

        boolean result = smartDeliveryClient.isDeliveryCompleted("04", "123456");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("응답 객체가 null인 경우 false를 반환한다")
    void isDeliveryCompleted_ResponseNull_ReturnsFalse() {
        setupWebClientMock(null);

        boolean result = smartDeliveryClient.isDeliveryCompleted("04", "123456");

        assertThat(result).isFalse();
    }

    private void setupWebClientMock(SmartTrackingResponse response) {
        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(Function.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.bodyToMono(SmartTrackingResponse.class)).willReturn(Mono.justOrEmpty(response));
    }
}