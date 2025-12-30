package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.PaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentCancelRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
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

    @InjectMocks
    private PaymentTransactionService paymentTransactionService;

    @Test
    @DisplayName("결제 내역 저장 성공 (Mock 필드 완벽 보충)")
    void savePayment_Success() {
        CommonResponse commonResponse = mock(CommonResponse.class);
        PaymentCreateRequest createRequest = mock(PaymentCreateRequest.class);

        // [핵심] Payment 엔티티 생성자에서 호출하는 모든 필드 값을 Mock으로 미리 설정
        given(createRequest.paymentMethod()).willReturn("카드");
        given(createRequest.paymentProvider()).willReturn("TOSS");
        given(createRequest.paymentStatus()).willReturn("DONE");
        given(createRequest.paymentCreatedAt()).willReturn(LocalDateTime.now());

        given(commonResponse.toPaymentCreateRequest("TOSS")).willReturn(createRequest);
        given(paymentRepository.save(any(Payment.class))).willReturn(mock(Payment.class));

        Payment result = paymentTransactionService.savePayment("TOSS", commonResponse);

        assertThat(result).isNotNull();
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 내역 저장 실패 시 재시도 후 예외 발생")
    void savePayment_RetryExceeded_ThrowsException() {
        CommonResponse commonResponse = mock(CommonResponse.class);
        PaymentCreateRequest createRequest = mock(PaymentCreateRequest.class);

        // 엔티티 생성 과정(valueOf 등)에서 에러가 나지 않도록 필드 보충
        given(createRequest.paymentMethod()).willReturn("카드");
        given(createRequest.paymentProvider()).willReturn("TOSS");
        given(createRequest.paymentStatus()).willReturn("DONE");
        given(createRequest.paymentCreatedAt()).willReturn(LocalDateTime.now());

        given(commonResponse.toPaymentCreateRequest(any())).willReturn(createRequest);
        given(paymentRepository.save(any(Payment.class))).willThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> paymentTransactionService.savePayment("TOSS", commonResponse))
                .isExactlyInstanceOf(PaymentException.class)
                .hasMessageContaining("재시도 횟수 초과");

        verify(paymentRepository, times(3)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 취소 내역 저장 성공")
    void savePaymentCancel_Success() {
        Cancel cancel = new Cancel(1000, "reason", LocalDateTime.now());
        // 생성자 인자 개수에 유의 (paymentKey, orderId/extra, cancels)
        PaymentCancelCreateRequest req = new PaymentCancelCreateRequest("pay-key", "order-123", List.of(cancel));

        PaymentCancel paymentCancel = mock(PaymentCancel.class);
        PaymentCancelResponse response = mock(PaymentCancelResponse.class);

        given(paymentCancelRepository.saveAll(anyList())).willReturn(List.of(paymentCancel));
        given(paymentCancel.toResponse()).willReturn(response);

        List<PaymentCancelResponse> result = paymentTransactionService.savePaymentCancel(req);

        assertThat(result).hasSize(1);
        verify(paymentCancelRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("결제 취소 내역 저장 실패 시 재시도 후 예외 발생")
    void savePaymentCancel_RetryExceeded_ThrowsException() {
        Cancel cancel = new Cancel(1000, "reason", LocalDateTime.now());
        PaymentCancelCreateRequest req = new PaymentCancelCreateRequest("pay-key", "order-123", List.of(cancel));

        given(paymentCancelRepository.saveAll(anyList())).willThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> paymentTransactionService.savePaymentCancel(req))
                .isExactlyInstanceOf(PaymentException.class)
                .hasMessageContaining("재시도 횟수 초과");

        verify(paymentCancelRepository, times(3)).saveAll(anyList());
    }
}