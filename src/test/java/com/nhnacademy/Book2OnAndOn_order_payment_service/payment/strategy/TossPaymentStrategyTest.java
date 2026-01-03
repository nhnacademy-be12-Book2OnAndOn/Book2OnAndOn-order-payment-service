package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.TossPaymentsApiClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.property.TossPaymentsProperties;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TossPaymentStrategyTest {

    @Mock
    private TossPaymentsApiClient tossPaymentsApiClient;

    @Mock
    private TossPaymentsProperties properties;

    @InjectMocks
    private TossPaymentStrategy tossPaymentStrategy;

    private final String SECRET_KEY = "test_secret_key";
    private String expectedAuthHeader;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getSecretKey()).thenReturn(SECRET_KEY);
        String encoded = Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes());
        expectedAuthHeader = "Basic " + encoded;
    }

    @Test
    @DisplayName("제공자 이름이 TOSS여야 한다")
    void getProviderTest() {
        assertThat(tossPaymentStrategy.getProvider()).isEqualTo("TOSS");
    }

    @Test
    @DisplayName("결제 승인 API 호출 및 응답 변환 성공")
    void confirmPayment_Success() {
        CommonConfirmRequest req = mock(CommonConfirmRequest.class);
        TossConfirmRequest tossReq = mock(TossConfirmRequest.class);
        TossResponse tossRes = mock(TossResponse.class);
        CommonResponse commonRes = mock(CommonResponse.class);
        String idempotencyKey = "uuid-key";

        given(req.toTossConfirmRequest()).willReturn(tossReq);
        given(tossPaymentsApiClient.confirmPayment(eq(expectedAuthHeader), eq(idempotencyKey), eq(tossReq)))
                .willReturn(tossRes);
        given(tossRes.toCommonConfirmResponse()).willReturn(commonRes);

        CommonResponse result = tossPaymentStrategy.confirmPayment(req, idempotencyKey);

        assertThat(result).isEqualTo(commonRes);
        verify(tossPaymentsApiClient).confirmPayment(anyString(), anyString(), any(TossConfirmRequest.class));
    }

    @Test
    @DisplayName("결제 취소 API 호출 및 응답 변환 성공")
    void cancelPayment_Success() {
        CommonCancelRequest req = mock(CommonCancelRequest.class);
        TossCancelRequest tossReq = mock(TossCancelRequest.class);
        TossCancelResponse tossRes = mock(TossCancelResponse.class);
        CommonCancelResponse commonRes = mock(CommonCancelResponse.class);
        String idempotencyKey = "uuid-key";

        given(req.paymentKey()).willReturn("pk_123");
        given(req.toTossCancelRequest()).willReturn(tossReq);
        given(tossPaymentsApiClient.cancelPayment(eq(expectedAuthHeader), eq(idempotencyKey), eq("pk_123"), eq(tossReq)))
                .willReturn(tossRes);
        given(tossRes.toCommonCancelResponse()).willReturn(commonRes);

        CommonCancelResponse result = tossPaymentStrategy.cancelPayment(req, idempotencyKey);

        assertThat(result).isEqualTo(commonRes);
        verify(tossPaymentsApiClient).cancelPayment(anyString(), anyString(), anyString(), any(TossCancelRequest.class));
    }
}