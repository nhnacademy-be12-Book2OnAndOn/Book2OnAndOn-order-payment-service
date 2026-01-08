package com.nhnacademy.book2onandon_order_payment_service.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhnacademy.book2onandon_order_payment_service.exception.PaymentException;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.PaymentCancel;
import com.nhnacademy.book2onandon_order_payment_service.payment.publisher.PaymentEventPublisher;
import com.nhnacademy.book2onandon_order_payment_service.payment.repository.PaymentCancelRepository;
import com.nhnacademy.book2onandon_order_payment_service.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentCancelRepository paymentCancelRepository;
    @Mock
    private OrderTransactionService orderTransactionService;
    @Mock
    private OrderResourceManager orderResourceManager;
    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentTransactionService paymentTransactionService;

    // ===================== 결제 저장 =====================

    @Test
    @DisplayName("결제 저장 성공")
    void savePaymentAndPublishEvent_success() {
        // given
        CommonResponse commonResponse = org.mockito.Mockito.mock(CommonResponse.class);
        PaymentCreateRequest createRequest =
                new PaymentCreateRequest(
                        "payment-key-123",        // paymentKey
                        "ORD-1",                  // orderNumber
                        10000,                    // totalAmount
                        "CARD",                   // paymentMethod
                        "TOSS",                   // paymentProvider
                        "DONE",                // paymentStatus
                        LocalDateTime.now(),      // paymentCreatedAt
                        "https://receipt.url",    // paymentReceiptUrl
                        0                          // refundAmount
                );

        Order order = org.mockito.Mockito.mock(Order.class);
        Payment savedPayment = org.mockito.Mockito.mock(Payment.class);

        when(commonResponse.toPaymentCreateRequest("TOSS")).thenReturn(createRequest);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // when
        Payment result =
                paymentTransactionService.savePaymentAndPublishEvent("TOSS", commonResponse, order);

        // then
        assertThat(result).isEqualTo(savedPayment);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderTransactionService, times(1)).changeStatusOrder(order, true);
        verify(orderResourceManager, times(1))
                .completeOrder(any(), any(), any(), any());
        verify(paymentEventPublisher, times(1))
                .publishSuccessPayment(order);
    }

    @Test
    @DisplayName("결제 저장 실패 시 PaymentException 발생")
    void savePaymentAndPublishEvent_fail() {
        // given
        CommonResponse commonResponse = org.mockito.Mockito.mock(CommonResponse.class);
        PaymentCreateRequest createRequest =
                new PaymentCreateRequest(
                        "payment-key-123",        // paymentKey
                        "ORD-1",                  // orderNumber
                        10000,                    // totalAmount
                        "CARD",                   // paymentMethod
                        "TOSS",                   // paymentProvider
                        "DONE",                // paymentStatus
                        LocalDateTime.now(),      // paymentCreatedAt
                        "https://receipt.url",    // paymentReceiptUrl
                        0                          // refundAmount
                );
        Order order = org.mockito.Mockito.mock(Order.class);

        when(commonResponse.toPaymentCreateRequest("TOSS")).thenReturn(createRequest);
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new RuntimeException("DB ERROR"));

        // then
        assertThatThrownBy(() ->
                paymentTransactionService.savePaymentAndPublishEvent("TOSS", commonResponse, order))
                .isExactlyInstanceOf(PaymentException.class);

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    // ===================== 결제 취소 저장 =====================

    @Test
    @DisplayName("결제 취소 저장 성공")
    void savePaymentCancel_success() {
        // given
        Cancel cancel = new Cancel(1000, "취소", LocalDateTime.now());
        PaymentCancelCreateRequest req =
                new PaymentCancelCreateRequest(
                        "pk",
                        "CANCELED",
                        List.of(cancel)
                );

        PaymentCancel paymentCancel = org.mockito.Mockito.mock(PaymentCancel.class);
        PaymentCancelResponse response = org.mockito.Mockito.mock(PaymentCancelResponse.class);

        when(paymentCancelRepository.saveAll(anyList()))
                .thenReturn(List.of(paymentCancel));
        when(paymentCancel.toResponse()).thenReturn(response);

        // when
        List<PaymentCancelResponse> result =
                paymentTransactionService.savePaymentCancel(req);

        // then
        assertThat(result).hasSize(1);
        verify(paymentCancelRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("결제 취소 저장 재시도 초과 시 PaymentException 발생")
    void savePaymentCancel_retryExceeded() {
        // given
        Cancel cancel = new Cancel(1000, "취소", LocalDateTime.now());
        PaymentCancelCreateRequest req =
                new PaymentCancelCreateRequest(
                        "pk",
                        "CANCELED",
                        List.of(cancel)
                );

        when(paymentCancelRepository.saveAll(anyList()))
                .thenThrow(new RuntimeException("DB ERROR"));

        // then
        assertThatThrownBy(() ->
                paymentTransactionService.savePaymentCancel(req))
                .isExactlyInstanceOf(PaymentException.class);

        // MAX_RETRY = 2 → 총 3회 시도
        verify(paymentCancelRepository, times(3)).saveAll(anyList());
    }
}
