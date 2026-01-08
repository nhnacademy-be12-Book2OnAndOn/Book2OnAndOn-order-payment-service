package com.nhnacademy.book2onandon_order_payment_service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api.TossCancelRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api.TossCancelResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api.TossConfirmRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api.TossResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TossPaymentsApiClientTest {

    private final TossPaymentsApiClient apiClient = mock(TossPaymentsApiClient.class);

    private final String auth = "Basic dGVzdF9za196RzBySjYyR3o3S242NE4wcm8zZWI1ZWRE";
    private final String idempotencyKey = "test-key-001";

    @Test
    @DisplayName("결제 승인 요청 시 필요한 파라미터가 정확히 전달되는지 확인한다")
    void confirmPayment_Call_Success() {
        TossConfirmRequest request = mock(TossConfirmRequest.class);
        TossResponse expectedResponse = mock(TossResponse.class);

        given(apiClient.confirmPayment(auth, idempotencyKey, request))
                .willReturn(expectedResponse);

        TossResponse actualResponse = apiClient.confirmPayment(auth, idempotencyKey, request);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(apiClient).confirmPayment(auth, idempotencyKey, request);
    }

    @Test
    @DisplayName("결제 취소 요청 시 경로 변수와 바디 데이터가 정확히 전달되는지 확인한다")
    void cancelPayment_Call_Success() {
        String paymentKey = "pay_key_123";
        TossCancelRequest request = mock(TossCancelRequest.class);
        TossCancelResponse expectedResponse = mock(TossCancelResponse.class);

        given(apiClient.cancelPayment(auth, idempotencyKey, paymentKey, request))
                .willReturn(expectedResponse);

        TossCancelResponse actualResponse = apiClient.cancelPayment(auth, idempotencyKey, paymentKey, request);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(apiClient).cancelPayment(auth, idempotencyKey, paymentKey, request);
    }

    @Test
    @DisplayName("주문 ID를 통한 결제 조회 요청이 정상적으로 호출되는지 확인한다")
    void findPayment_Call_Success() {
        String orderId = "ORD-2025-001";
        TossResponse expectedResponse = mock(TossResponse.class);

        given(apiClient.findPayment(auth, orderId))
                .willReturn(expectedResponse);

        TossResponse actualResponse = apiClient.findPayment(auth, orderId);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(apiClient).findPayment(auth, orderId);
    }
}