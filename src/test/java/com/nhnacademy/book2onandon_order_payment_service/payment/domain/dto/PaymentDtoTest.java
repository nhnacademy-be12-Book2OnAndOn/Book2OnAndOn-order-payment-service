package com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api.*;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentDtoTest {

    @Test
    @DisplayName("TossResponse -> CommonResponse 변환")
    void tossResponse_toCommonResponse() {
        TossResponse.Receipt receipt = new TossResponse.Receipt("url");
        TossResponse tossResponse = new TossResponse(
                "ORD-1", 1000, "CARD", "DONE", LocalDateTime.now(), receipt, "PK-1"
        );

        CommonResponse commonResponse = tossResponse.toCommonConfirmResponse();
        assertThat(commonResponse.paymentKey()).isEqualTo("PK-1");
        assertThat(commonResponse.orderId()).isEqualTo("ORD-1");
        assertThat(commonResponse.receiptUrl()).isEqualTo("url");
    }

    @Test
    @DisplayName("TossCancelResponse -> CommonCancelResponse 변환")
    void tossCancelResponse_toCommonCancelResponse() {
        Cancel cancel = new Cancel(500, "취소사유", LocalDateTime.now());
        TossCancelResponse tossCancelResponse = new TossCancelResponse("PK-1", "CANCELED", List.of(cancel));

        CommonCancelResponse commonCancelResponse = tossCancelResponse.toCommonCancelResponse();

        assertThat(commonCancelResponse.paymentKey()).isEqualTo("PK-1");
        assertThat(commonCancelResponse.status()).isEqualTo("CANCELED");
        assertThat(commonCancelResponse.cancels()).hasSize(1);
    }

    @Test
    @DisplayName("CommonResponse -> PaymentCreateRequest 변환")
    void commonResponse_toPaymentCreateRequest() {
        CommonResponse commonResponse = new CommonResponse(
                "PK-1", "ORD-1", 1000, "CARD", "DONE", LocalDateTime.now(), "url"
        );

        PaymentCreateRequest request = commonResponse.toPaymentCreateRequest("TOSS");
        assertThat(request.paymentKey()).isEqualTo("PK-1");
        assertThat(request.paymentProvider()).isEqualTo("TOSS");
        assertThat(request.paymentStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("CommonConfirmRequest -> TossConfirmRequest 변환")
    void commonConfirmRequest_toTossConfirmRequest() {
        CommonConfirmRequest commonConfirmRequest = new CommonConfirmRequest("ORD-1", "PK-1", 1000);
        TossConfirmRequest tossConfirmRequest = commonConfirmRequest.toTossConfirmRequest();

        assertThat(tossConfirmRequest.orderId()).isEqualTo("ORD-1");
        assertThat(tossConfirmRequest.amount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("CommonCancelRequest -> TossCancelRequest 변환")
    void commonCancelRequest_toTossCancelRequest() {
        CommonCancelRequest commonCancelRequest = new CommonCancelRequest("PK-1", 500, "취소사유");
        TossCancelRequest tossCancelRequest = commonCancelRequest.toTossCancelRequest();

        assertThat(tossCancelRequest.cancelAmount()).isEqualTo(500);
        assertThat(tossCancelRequest.cancelReason()).isEqualTo("취소사유");
    }

    @Test
    @DisplayName("CommonCancelResponse -> PaymentCancelCreateRequest 변환")
    void commonCancelResponse_toPaymentCancelCreateRequest() {
        Cancel cancel = new Cancel(500, "취소사유", LocalDateTime.now());
        CommonCancelResponse commonCancelResponse = new CommonCancelResponse(
                "PK-1", "CANCELED", List.of(cancel)
        );

        PaymentCancelCreateRequest req = commonCancelResponse.toPaymentCancelCreateRequest();
        assertThat(req.paymentKey()).isEqualTo("PK-1");
        assertThat(req.cancels()).hasSize(1);
    }

    @Test
    @DisplayName("PaymentCancelResponse 생성자 값 확인")
    void paymentCancelResponse_values() {
        LocalDateTime now = LocalDateTime.now();
        PaymentCancelResponse response = new PaymentCancelResponse("PK-1", 500, "취소사유", now);

        assertThat(response.paymentKey()).isEqualTo("PK-1");
        assertThat(response.cancelAmount()).isEqualTo(500);
        assertThat(response.cancelReason()).isEqualTo("취소사유");
        assertThat(response.canceledAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("PaymentResponse 생성자 값 확인")
    void paymentResponse_values() {
        LocalDateTime now = LocalDateTime.now();
        PaymentCancelResponse cancelResponse = new PaymentCancelResponse("PK-1", 500, "취소사유", now);

        PaymentResponse response = new PaymentResponse(
                "PK-1",
                "ORD-1",
                1000,
                "CARD",
                "TOSS",
                "DONE",
                now,
                "url",
                0,
                List.of(cancelResponse)
        );

        assertThat(response.paymentKey()).isEqualTo("PK-1");
        assertThat(response.orderNumber()).isEqualTo("ORD-1");
        assertThat(response.totalAmount()).isEqualTo(1000);
        assertThat(response.paymentCancelResponseList()).hasSize(1);
    }

    @Test
    @DisplayName("PaymentUpdateRefundAmountRequest 생성 및 값 확인")
    void paymentUpdateRefundAmountRequest_values() {
        // given
        String orderNumber = "ORD-001";
        String paymentKey = "PAY-12345";

        // when
        PaymentUpdateRefundAmountRequest request = new PaymentUpdateRefundAmountRequest(orderNumber, paymentKey);

        // then
        assertThat(request.orderNumber()).isEqualTo(orderNumber);
        assertThat(request.paymentKey()).isEqualTo(paymentKey);
    }
}
