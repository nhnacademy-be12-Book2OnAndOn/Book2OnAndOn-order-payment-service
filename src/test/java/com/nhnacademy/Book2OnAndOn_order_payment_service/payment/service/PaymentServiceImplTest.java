package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentDeleteRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdatePaymentStatusRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentDeleteResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.DuplicatePaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class PaymentServiceImplTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp(){
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("결제 데이터 생성 성공")
    void createPayment_Success() {
        PaymentCreateRequest req = new PaymentCreateRequest(
                "testkey",
                "B20000000001",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                null,
                "http://test.com",
                0
        );

        PaymentResponse response = paymentService.createPayment(req);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("B20000000001", response.orderNumber());
        Assertions.assertEquals("결제 성공", response.paymentStatus());
    }

    @Test
    @DisplayName("결제 데이터 생성 실패 - 중복 데이터")
    void createPayment_Failure_Duplicate() {
        PaymentCreateRequest req = new PaymentCreateRequest(
                "testkey",
                "B20000000001",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                null,
                "http://test.com",
                0
        );

        PaymentCreateRequest req2 = new PaymentCreateRequest(
                "testkey2",
                "B20000000001",
                20000,
                "CARD",
                "TOSS",
                "DONE",
                null,
                "http://test2.com",
                0
        );

        PaymentResponse response = paymentService.createPayment(req);

        Assertions.assertThrows(DuplicatePaymentException.class, () -> {
            paymentService.createPayment(req2);
        });
    }

    @Test
    @DisplayName("결제 내역 가져오기 - 성공")
    void getPayment_Success(){
        PaymentCreateRequest req = new PaymentCreateRequest(
                "testkey",
                "B20000000001",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                null,
                "http://test.com",
                0
        );

        paymentService.createPayment(req);

        PaymentRequest paymentRequest = new PaymentRequest("B20000000001");

        PaymentResponse response = paymentService.getPayment(paymentRequest);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("B20000000001", response.orderNumber());
        Assertions.assertEquals("결제 성공", response.paymentStatus());
    }

    @Test
    @DisplayName("결제 내역 가져오기 실패 - 데이터 없음")
    void getPayment_Failure_NotFound(){
        PaymentRequest req = new PaymentRequest("B20000000001");

        Assertions.assertThrows(NotFoundPaymentException.class, () -> {
            paymentService.getPayment(req);
        });
    }

    @Test
    @DisplayName("결제 내역 삭제 - 성공")
    void deletePayment_Success(){
        PaymentCreateRequest req = new PaymentCreateRequest(
                "testkey",
                "B20000000001",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                null,
                "http://test.com",
                0
        );

        paymentService.createPayment(req);

        PaymentDeleteRequest deleteRequest = new PaymentDeleteRequest("B20000000001");
        PaymentRequest paymentRequest = new PaymentRequest("B20000000001");

        PaymentDeleteResponse response = paymentService.deletePayment(deleteRequest);

        Assertions.assertEquals("B20000000001", response.orderNumber());
        Assertions.assertThrows(NotFoundPaymentException.class, () -> paymentService.getPayment(paymentRequest));
    }

    @Test
    @DisplayName("결제 내역 업데이트 - 환불 금액 추가")
    void updatePayment_AddRefundAmount(){
        PaymentCreateRequest req = new PaymentCreateRequest(
                "testKey",
                "B20000000001",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                null,
                "http://test.com",
                0
        );


        List<Cancel> cancels = new ArrayList<>();
        PaymentCancelCreateRequest cancelCreateReq = new PaymentCancelCreateRequest(
                "testKey","testStatus", cancels);

        paymentService.createPayment(req);
        paymentService.createPaymentCancel(cancelCreateReq);

        PaymentUpdateRefundAmountRequest updateRefundAmountRequest = new PaymentUpdateRefundAmountRequest("testKey", "B20000000001");

        PaymentResponse response = paymentService.updateRefundAmount(updateRefundAmountRequest);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(3000, response.refundAmount());
    }

    @Test
    @DisplayName("결제 내역 업데이트 - 결제 상태 변경")
    void updatePayment_ChangePaymentStatus(){
        PaymentCreateRequest req = new PaymentCreateRequest(
                "testkey",
                "B20000000005",
                10000,
                "CARD",
                "TOSS",
                "DONE",
                null,
                "http://test.com",
                0
        );

        paymentService.createPayment(req);

        PaymentUpdatePaymentStatusRequest paymentUpdatePaymentStatusRequest = new PaymentUpdatePaymentStatusRequest("B20000000001", "CANCELED");

        PaymentResponse response = paymentService.updatePaymentStatus(paymentUpdatePaymentStatusRequest);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("결제 취소", response.paymentStatus());
    }
}
