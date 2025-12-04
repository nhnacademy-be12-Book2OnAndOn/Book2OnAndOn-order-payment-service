package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.DuplicatePaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentCancelRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentCancelRepository paymentCancelRepository;

    @Mock
    private OrderService orderService;

    @Test
    @DisplayName("결제 정보 생성 성공")
    void createPayment_Success() {
        // given
        PaymentCreateRequest req = new PaymentCreateRequest(
                "paymentKey_123", "ORDER_001", 10000, "CARD", "TOSS", "DONE", "2024-01-01 10:10:10", "url", 0
        );
        Payment payment = new Payment(req);

        // Mock: 주문번호로 조회 시 null 반환 (중복 없음)
        given(paymentRepository.findByOrderNumber("ORDER_001")).willReturn(null);
        // Mock: 저장 시 엔티티 반환
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);

        // when
        PaymentResponse response = paymentService.createPayment(req);

        // then
        assertThat(response.orderNumber()).isEqualTo("ORDER_001");
        assertThat(response.totalAmount()).isEqualTo(10000);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 정보 생성 실패 - 이미 존재하는 결제")
    void createPayment_Fail_Duplicate() {
        // given
        PaymentCreateRequest req = new PaymentCreateRequest(
                "key", "ORDER_DUP", 10000, "CARD", "TOSS", "DONE", null, null, 0
        );
        Payment existingPayment = new Payment(req);

        // Mock: 이미 결제 정보가 존재함
        given(paymentRepository.findByOrderNumber("ORDER_DUP")).willReturn(existingPayment);

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(req))
                .isInstanceOf(DuplicatePaymentException.class);
    }

    @Test
    @DisplayName("결제 취소 내역 저장 성공")
    void createPaymentCancel_Success() {
        // given
        Cancel cancelDto = new Cancel(10000, "변심", 10000, 0, 10000, "key", "id", 0);
        PaymentCancelCreateRequest req = new PaymentCancelCreateRequest(
                "paymentKey_123", "CANCELED", List.of(cancelDto)
        );

        PaymentCancel paymentCancel = new PaymentCancel("paymentKey_123", 10000, "변심", "2024-01-01");

        // Mock: 저장된 취소 리스트 반환
        given(paymentCancelRepository.saveAll(any())).willReturn(List.of(paymentCancel));

        // when
        List<PaymentCancelResponse> responses = paymentService.createPaymentCancel(req);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).cancelAmount()).isEqualTo(10000);
        assertThat(responses.get(0).cancelReason()).isEqualTo("변심");
    }

    @Test
    @DisplayName("주문 금액 검증 - 일치")
    void validateOrderAmount_Match() {
        // given
        String orderId = "ORDER_001";
        Integer amount = 5000;

        // Mock: OrderService가 실제 주문 금액 5000원을 리턴
        given(orderService.getTotalAmount(orderId)).willReturn(5000);

        // when
//        boolean result = paymentService.validateOrderAmount(orderId, amount);

        // then
//        assertThat(result).isTrue();
    }
}