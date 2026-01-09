package com.nhnacademy.book2onandon_order_payment_service.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhnacademy.book2onandon_order_payment_service.exception.PaymentException;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentCancelRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.PaymentProvider;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.PaymentStatus;
import com.nhnacademy.book2onandon_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.book2onandon_order_payment_service.payment.repository.PaymentRepository;
import com.nhnacademy.book2onandon_order_payment_service.payment.service.PaymentTransactionService;
import com.nhnacademy.book2onandon_order_payment_service.payment.strategy.PaymentStrategy;
import com.nhnacademy.book2onandon_order_payment_service.payment.strategy.PaymentStrategyFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentStrategyFactory factory;
    @Mock private PaymentTransactionService paymentTransactionService;
    @Mock private OrderTransactionService orderTransactionService;
    @Mock private OrderResourceManager orderResourceManager;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    /* ================= 결제 조회 ================= */

    @Test
    void 결제_조회_성공() {
        Payment payment = mock(Payment.class);
        PaymentResponse response = mock(PaymentResponse.class);

        given(paymentRepository.findByOrderNumber("ORD-1"))
                .willReturn(Optional.of(payment));
        given(payment.getPaymentStatus()).willReturn(PaymentStatus.SUCCESS);
        given(payment.toResponse()).willReturn(response);

        PaymentResponse result =
                paymentService.getPayment(new PaymentRequest("ORD-1"));

        assertThat(result).isEqualTo(response);
    }

    @Test
    void 결제_조회_실패_존재하지않음() {
        given(paymentRepository.findByOrderNumber("ORD-1"))
                .willReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest("ORD-1");

        assertThatThrownBy(() -> paymentService.getPayment(request))
                .isInstanceOf(NotFoundPaymentException.class);
    }

    /* ================= 결제 승인 ================= */

    @Test
    void 결제_승인_성공() {
        // given
        CommonConfirmRequest req =
                new CommonConfirmRequest("ORD-1", "pk", 1000);

        Order order = mock(Order.class);
        PaymentResponse response = mock(PaymentResponse.class);
        PaymentStrategy strategy = mock(PaymentStrategy.class);
        CommonResponse commonResponse = mock(CommonResponse.class);
        Payment savedPayment = mock(Payment.class);

        given(orderTransactionService.getOrderEntity("ORD-1"))
                .willReturn(order);
        given(orderTransactionService.validateOrderAmount(req))
                .willReturn(order);

        given(factory.getStrategy("toss"))
                .willReturn(strategy);
        given(strategy.confirmPayment(eq(req), anyString()))
                .willReturn(commonResponse);

        given(paymentTransactionService
                .savePaymentAndPublishEvent("toss", commonResponse, order))
                .willReturn(savedPayment);

        given(savedPayment.toResponse())
                .willReturn(response);

        // when
        PaymentResponse result =
                paymentService.confirmAndCreatePayment("toss", req);

        // then
        assertThat(result).isEqualTo(response);
    }

    @Test
    void 결제_승인_재시도_초과() {
        CommonConfirmRequest req =
                new CommonConfirmRequest("ORD-1", "pk", 1000);

        Order order = mock(Order.class);
        PaymentStrategy strategy = mock(PaymentStrategy.class);

        given(orderTransactionService.getOrderEntity("ORD-1")).willReturn(order);
        given(orderTransactionService.validateOrderAmount(req)).willReturn(order);

        given(factory.getStrategy("toss")).willReturn(strategy);
        given(strategy.confirmPayment(eq(req), anyString()))
                .willThrow(new RuntimeException("API ERROR"));

        assertThatThrownBy(() ->
                paymentService.confirmAndCreatePayment("toss", req))
                .isInstanceOf(PaymentException.class);

        verify(strategy, times(5))
                .confirmPayment(eq(req), anyString());
    }

    @Test
    void 결제_승인_중_주문금액_검증_실패시_보상로직_실행() {
        // given
        CommonConfirmRequest req =
                new CommonConfirmRequest("ORD-1", "pk", 1000);

        Order order = mock(Order.class);

        given(order.getOrderNumber()).willReturn("ORD-1");
        given(order.getUserId()).willReturn(1L);
        given(order.getPointDiscount()).willReturn(500);
        given(order.getOrderId()).willReturn(10L);

        given(orderTransactionService.getOrderEntity("ORD-1"))
                .willReturn(order);

        given(orderTransactionService.validateOrderAmount(req))
                .willThrow(new IllegalArgumentException("금액 불일치"));

        // when & then
        assertThatThrownBy(() ->
                paymentService.confirmAndCreatePayment("toss", req))
                .isInstanceOf(IllegalArgumentException.class);

        // 보상 로직 검증
        verify(orderResourceManager).releaseResources(
                "ORD-1",
                1L,
                500,
                10L
        );

        verify(orderTransactionService)
                .changeStatusOrder(order, false);

        // 승인 로직은 호출되지 않아야 함
        verify(factory, never()).getStrategy(any());
    }


    /* ================= 결제 삭제 ================= */

    @Test
    void 결제_삭제_성공() {
        Payment payment = mock(Payment.class);

        given(paymentRepository.findByOrderNumber("ORD-1"))
                .willReturn(Optional.of(payment));

        paymentService.deletePayment(
                new PaymentDeleteRequest("ORD-1"));

        verify(paymentRepository).delete(payment);
    }

    @Test
    @DisplayName("결제 삭제 실패 - 결제 존재하지 않음")
    void 결제_삭제_실패() {
        // given
        String orderNumber = "ORD-999"; // 존재하지 않는 결제 번호
        PaymentDeleteRequest request = new PaymentDeleteRequest(orderNumber);

        // findByOrderNumber 호출 시 NotFoundPaymentException 발생하도록 설정
        when(paymentRepository.findByOrderNumber(orderNumber))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.deletePayment(request))
                .isInstanceOf(NotFoundPaymentException.class)
                .hasMessageContaining("Not Found Payment");

        // delete는 호출되지 않아야 함
        verify(paymentRepository, never()).delete(any());
    }

    /* ================= 결제 취소 ================= */

    @Test
    void 결제_취소_성공() {
        Payment payment = mock(Payment.class);
        PaymentStrategy strategy = mock(PaymentStrategy.class);
        CommonCancelResponse cancelResponse = mock(CommonCancelResponse.class);
        PaymentCancelResponse cancel = mock(PaymentCancelResponse.class);

        given(paymentRepository.findByOrderNumber("ORD-1"))
                .willReturn(Optional.of(payment));
        given(payment.getPaymentProvider())
                .willReturn(PaymentProvider.TOSS);
        given(payment.getPaymentKey()).willReturn("pk");

        given(factory.getStrategy("TOSS")).willReturn(strategy);
        given(strategy.cancelPayment(any(), anyString()))
                .willReturn(cancelResponse);

        given(cancelResponse.toPaymentCancelCreateRequest())
                .willReturn(mock(PaymentCancelCreateRequest.class));

        given(cancel.cancelAmount()).willReturn(1000);
        given(paymentTransactionService.savePaymentCancel(any()))
                .willReturn(List.of(cancel));

        paymentService.cancelPayment(
                new PaymentCancelRequest("ORD-1", "취소", 1000));

        verify(payment).setRefundAmount(1000);
    }

    @Test
    @DisplayName("결제 취소 재시도 초과 시 PaymentException 발생")
    void 결제_취소_재시도_초과시_예외발생() {
        // given
        Payment payment = mock(Payment.class);
        PaymentStrategy strategy = mock(PaymentStrategy.class);

        given(paymentRepository.findByOrderNumber("ORD-1"))
                .willReturn(Optional.of(payment));
        given(payment.getPaymentProvider())
                .willReturn(PaymentProvider.TOSS);
        given(payment.getPaymentKey())
                .willReturn("pk");
        given(factory.getStrategy("TOSS"))
                .willReturn(strategy);

        // cancelPayment가 항상 RuntimeException 발생
        given(strategy.cancelPayment(any(), anyString()))
                .willThrow(new RuntimeException("API ERROR"));

        PaymentCancelRequest request = new PaymentCancelRequest("ORD-1", "취소", 1000);

        // when & then
        assertThatThrownBy(() -> paymentService.cancelPayment(request))
                .isInstanceOf(PaymentException.class);

        // 재시도 횟수 검증 (5회 시도)
        verify(strategy, times(5)).cancelPayment(any(), anyString());
    }

}
