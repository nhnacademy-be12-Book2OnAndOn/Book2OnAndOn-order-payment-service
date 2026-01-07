package com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentEntityTest {

    @Test
    void PaymentCreateRequest로_엔티티_생성_성공() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PaymentCreateRequest req = new PaymentCreateRequest(
                "pk",
                "ORD-1",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                now,
                "receipt-url",
                0
        );

        // when
        Payment payment = new Payment(req);

        // then
        assertThat(payment.getPaymentKey()).isEqualTo("pk");
        assertThat(payment.getOrderNumber()).isEqualTo("ORD-1");
        assertThat(payment.getTotalAmount()).isEqualTo(10000);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getPaymentProvider()).isEqualTo(PaymentProvider.TOSS);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaymentCreatedAt()).isEqualTo(now);
        assertThat(payment.getPaymentReceiptUrl()).isEqualTo("receipt-url");
        assertThat(payment.getRefundAmount()).isEqualTo(0);
    }

    // =========================
    // 2. refundAmount 기본값 테스트
    // =========================

    @Test
    void refundAmount_null이면_기본값_0으로_설정된다() {
        // given
        PaymentCreateRequest req = new PaymentCreateRequest(
                "pk",
                "ORD-1",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                LocalDateTime.now(),
                "receipt-url",
                null
        );

        // when
        Payment payment = new Payment(req);

        // then
        assertThat(payment.getRefundAmount()).isEqualTo(0);
    }

    // =========================
    // 3. paymentCreatedAt 기본값 테스트
    // =========================

    @Test
    void paymentCreatedAt_null이면_현재시간으로_설정된다() {
        // given
        PaymentCreateRequest req = new PaymentCreateRequest(
                "pk",
                "ORD-1",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                null,
                "receipt-url",
                0
        );

        // when
        Payment payment = new Payment(req);

        // then
        assertThat(payment.getPaymentCreatedAt()).isNotNull();
    }

    // =========================
    // 4. toResponse 변환 테스트
    // =========================

    @Test
    void Payment_toResponse_변환_성공() {
        // given
        Payment payment = new Payment(
                "pk",
                "ORD-1",
                10000,
                PaymentMethod.CARD,
                PaymentProvider.TOSS,
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                "receipt-url",
                1000
        );

        // when
        PaymentResponse response = payment.toResponse();

        // then
        assertThat(response.paymentKey()).isEqualTo("pk");
        assertThat(response.orderNumber()).isEqualTo("ORD-1");
        assertThat(response.totalAmount()).isEqualTo(10000);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD.getDescription());
        assertThat(response.paymentProvider()).isEqualTo("TOSS");
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS.getDescription());
        assertThat(response.refundAmount()).isEqualTo(1000);
    }
}