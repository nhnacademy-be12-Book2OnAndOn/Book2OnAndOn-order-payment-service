//package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;
//
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentCancel;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.DuplicatePaymentException;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentCancelRepository;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.impl.PaymentServiceImpl;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//class PaymentServiceImplTest {
//
//    @InjectMocks
//    private PaymentServiceImpl paymentService;
//
//    @Mock
//    private PaymentRepository paymentRepository;
//
//    @Mock
//    private PaymentCancelRepository paymentCancelRepository;
//
//    @Test
//    @DisplayName("결제 정보 생성 성공")
//    void createPayment_Success() {
//        // given
//        PaymentCreateRequest req = new PaymentCreateRequest(
//                "paymentKey_123", "ORDER_001", 10000, "CARD", "TOSS", "DONE", LocalDateTime.now(), "http://url", 0
//        );
//        // Mock: 저장된 엔티티 반환 설정
//        Payment savedPayment = new Payment(req);
//        given(paymentRepository.findByOrderNumber(req.orderNumber())).willReturn(null); // 중복 없음
//        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);
//
//        // when
//        PaymentResponse response = paymentService.createPayment(req);
//
//        // then
//        assertThat(response.paymentKey()).isEqualTo("paymentKey_123");
//        assertThat(response.orderNumber()).isEqualTo("ORDER_001");
//        assertThat(response.totalAmount()).isEqualTo(10000);
//        verify(paymentRepository, times(1)).save(any(Payment.class));
//    }
//
//    @Test
//    @DisplayName("결제 정보 생성 실패 - 이미 존재하는 결제")
//    void createPayment_Fail_Duplicate() {
//        // given
//        PaymentCreateRequest req = new PaymentCreateRequest(
//                "key", "ORDER_DUP", 10000, "CARD", "TOSS", "DONE", LocalDateTime.now(), null, 0
//        );
//        Payment existingPayment = new Payment(req);
//
//        // Mock: 이미 결제 정보가 존재함
//        given(paymentRepository.findByOrderNumber("ORDER_DUP").orElse(null)).willReturn(existingPayment);
//
//        // when & then
//        assertThatThrownBy(() -> paymentService.createPayment(req))
//                .isInstanceOf(DuplicatePaymentException.class);
//    }
//
//    @Test
//    @DisplayName("결제 취소 내역 생성 성공")
//    void createPaymentCancel_Success() {
//        // given
//        Cancel cancelDto = new Cancel(5000, "단순 변심", LocalDateTime.now());
//        PaymentCancelCreateRequest req = new PaymentCancelCreateRequest(
//                "paymentKey_123", "CANCELED", List.of(cancelDto)
//        );
//
//        PaymentCancel paymentCancel = new PaymentCancel("paymentKey_123", 5000, "단순 변심", LocalDateTime.now());
//        given(paymentCancelRepository.saveAll(any())).willReturn(List.of(paymentCancel));
//
//        // when
//        List<PaymentCancelResponse> responses = paymentService.createPaymentCancel(req);
//
//        // then
//        assertThat(responses).hasSize(1);
//        assertThat(responses.get(0).cancelAmount()).isEqualTo(5000);
//        assertThat(responses.get(0).cancelReason()).isEqualTo("단순 변심");
//    }
//}