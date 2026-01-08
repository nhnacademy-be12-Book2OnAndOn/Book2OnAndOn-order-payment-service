package com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentCancelEntityTest {

    // =========================
    // 1. 생성자 테스트 (정상 값)
    // =========================

    @Test
    @DisplayName("PaymentCancel 생성자 - 모든 값 정상 설정")
    void createPaymentCancel_success() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        PaymentCancel paymentCancel =
                new PaymentCancel("pk", 1000, "사용자 요청", now);

        // then
        assertThat(paymentCancel.getPaymentKey()).isEqualTo("pk");
        assertThat(paymentCancel.getCancelAmount()).isEqualTo(1000);
        assertThat(paymentCancel.getCancelReason()).isEqualTo("사용자 요청");
        assertThat(paymentCancel.getCanceledAt()).isEqualTo(now);
    }

    // =========================
    // 2. canceledAt null 분기 테스트
    // =========================

    @Test
    @DisplayName("canceledAt이 null이면 현재 시간으로 설정된다")
    void canceledAt_null_then_now() {
        // when
        PaymentCancel paymentCancel =
                new PaymentCancel("pk", 1000, "사유", null);

        // then
        assertThat(paymentCancel.getCanceledAt()).isNotNull();
    }

    // =========================
    // 3. toResponse 변환 테스트
    // =========================

    @Test
    @DisplayName("PaymentCancel toResponse 변환 성공")
    void toResponse_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PaymentCancel paymentCancel =
                new PaymentCancel("pk", 5000, "부분 취소", now);

        // when
        PaymentCancelResponse response = paymentCancel.toResponse();

        // then
        assertThat(response.paymentKey()).isEqualTo("pk");
        assertThat(response.cancelAmount()).isEqualTo(5000);
        assertThat(response.cancelReason()).isEqualTo("부분 취소");
        assertThat(response.canceledAt()).isEqualTo(now);
    }
}
