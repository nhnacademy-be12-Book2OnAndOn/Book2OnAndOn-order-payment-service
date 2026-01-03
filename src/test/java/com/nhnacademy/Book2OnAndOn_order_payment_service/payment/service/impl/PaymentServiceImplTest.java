//package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.impl;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.PaymentException;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelRequest;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentProvider;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentStatus;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.publisher.PaymentEventPublisher;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentTransactionService;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategy;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;
//import java.util.List;
//import java.util.Optional;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//class PaymentServiceImplTest {
//
//    @Mock private PaymentRepository paymentRepository;
//    @Mock private PaymentStrategyFactory factory;
//    @Mock private PaymentTransactionService paymentTransactionService;
//    @Mock private OrderTransactionService orderTransactionService;
//    @Mock private OrderResourceManager orderResourceManager;
//    @Mock private PaymentEventPublisher paymentEventPublisher;
//
//    @InjectMocks
//    private PaymentServiceImpl paymentService;
//
//    @Test
//    @DisplayName("결제 조회 성공")
//    void getPayment_Success() {
//        PaymentRequest req = new PaymentRequest("ORD-1");
//        Payment payment = mock(Payment.class);
//        given(payment.getOrderNumber()).willReturn("ORD-1");
//        given(payment.getPaymentStatus()).willReturn(PaymentStatus.SUCCESS);
//        given(paymentRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(payment));
//        given(payment.toResponse()).willReturn(mock(PaymentResponse.class));
//
//        PaymentResponse result = paymentService.getPayment(req);
//
//        assertThat(result).isNotNull();
//    }
//
//    @Test
//    @DisplayName("결제 조회 실패 - 존재하지 않음")
//    void getPayment_NotFound() {
//        given(paymentRepository.findByOrderNumber(anyString())).willReturn(Optional.empty());
//        assertThatThrownBy(() -> paymentService.getPayment(new PaymentRequest("X")))
//                .isInstanceOf(NotFoundPaymentException.class);
//    }
//
//    @Test
//    @DisplayName("결제 승인 및 생성 성공")
//    void confirmAndCreatePayment_Success() {
//        CommonConfirmRequest req = new CommonConfirmRequest("ORD-1", "pk", 1000);
//        Order order = mock(Order.class);
//        PaymentStrategy strategy = mock(PaymentStrategy.class);
//        CommonResponse commonResponse = mock(CommonResponse.class);
//        Payment payment = mock(Payment.class);
//
//        given(orderTransactionService.validateOrderAmount(req)).willReturn(order);
//        given(factory.getStrategy("toss")).willReturn(strategy);
//        given(strategy.confirmPayment(eq(req), anyString())).willReturn(commonResponse);
//        given(paymentTransactionService.savePayment("toss", commonResponse)).willReturn(payment);
//        given(payment.toResponse()).willReturn(mock(PaymentResponse.class));
//
//        paymentService.confirmAndCreatePayment("toss", req);
//
//        verify(orderTransactionService).changeStatusOrder(order, true);
//        verify(orderResourceManager).finalizeBooks("ORD-1");
//        verify(paymentEventPublisher).publishSuccessPayment(order);
//    }
//
//    @Test
//    @DisplayName("결제 승인 실패 - 5회 재시도 초과")
//    void confirmPayment_RetryExceeded() {
//        CommonConfirmRequest req = new CommonConfirmRequest("ORD-1", "pk", 1000);
//        PaymentStrategy strategy = mock(PaymentStrategy.class);
//
//        given(orderTransactionService.validateOrderAmount(req)).willReturn(mock(Order.class));
//        given(factory.getStrategy("toss")).willReturn(strategy);
//        given(strategy.confirmPayment(eq(req), anyString())).willThrow(new RuntimeException("API Error"));
//
//        assertThatThrownBy(() -> paymentService.confirmAndCreatePayment("toss", req))
//                .isInstanceOf(PaymentException.class)
//                .hasMessageContaining("최대 재시도 횟수 초과");
//
//        verify(strategy, times(5)).confirmPayment(eq(req), anyString());
//    }
//
//    @Test
//    @DisplayName("결제 삭제 성공")
//    void deletePayment_Success() {
//        Payment payment = mock(Payment.class);
//        given(payment.getOrderNumber()).willReturn("ORD-1");
//        given(paymentRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(payment));
//
//        paymentService.deletePayment(new PaymentDeleteRequest("ORD-1"));
//
//        verify(paymentRepository).delete(payment);
//    }
//
//    @Test
//    @DisplayName("결제 취소 로직 성공")
//    void cancelPayment_Success() {
//        PaymentCancelRequest req = new PaymentCancelRequest("ORD-1", "reason", 1000);
//        Payment payment = mock(Payment.class);
//        PaymentStrategy strategy = mock(PaymentStrategy.class);
//        CommonCancelResponse cancelResponse = mock(CommonCancelResponse.class);
//
//        PaymentCancelResponse pcr = mock(PaymentCancelResponse.class);
//        given(pcr.cancelAmount()).willReturn(1000);
//
//        given(paymentRepository.findByOrderNumber("ORD-1")).willReturn(Optional.of(payment));
//        given(payment.getPaymentProvider()).willReturn(PaymentProvider.TOSS);
//        given(payment.getPaymentKey()).willReturn("pk");
//        given(factory.getStrategy("TOSS")).willReturn(strategy);
//        given(strategy.cancelPayment(any(CommonCancelRequest.class), anyString())).willReturn(cancelResponse);
//        given(cancelResponse.toPaymentCancelCreateRequest()).willReturn(mock(PaymentCancelCreateRequest.class));
//        given(paymentTransactionService.savePaymentCancel(any())).willReturn(List.of(pcr));
//
//        paymentService.cancelPayment(req);
//
//        verify(payment).setRefundAmount(1000);
//    }
//}