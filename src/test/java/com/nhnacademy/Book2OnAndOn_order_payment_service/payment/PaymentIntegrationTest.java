package com.nhnacademy.Book2OnAndOn_order_payment_service.payment;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.client.TossPaymentsApiClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.TossConfirmResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Spring Boot 3.4+
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 외부 결제 API는 Mocking
    @MockitoBean
    private TossPaymentsApiClient tossPaymentsApiClient;

    // 주문 서비스 의존성 Mocking
    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("통합 테스트: 결제 승인 성공")
    @WithMockUser
    void confirmPayment_Integration_Success() throws Exception {
        // given
        String orderId = "ORDER_INT_001";
        String paymentKey = "payment_key_int";
        int amount = 20000;

        // OrderService 검증 통과 (금액 일치)
        given(orderService.getTotalAmount(orderId)).willReturn(amount);

        // Toss API 응답 Mocking
        TossConfirmResponse tossResponse = new TossConfirmResponse(
                orderId,
                amount,
                "카드",
                "DONE",
                LocalDateTime.now(),
                new TossConfirmResponse.Receipt("http://url"),
                paymentKey
        );
        given(tossPaymentsApiClient.confirmPayment(anyString(), any(TossConfirmRequest.class)))
                .willReturn(tossResponse);

        // when
        mockMvc.perform(get("/payment/TOSS/confirm")
                        .header("X-USER-ID", 1L)
                        .param("orderId", orderId)
                        .param("paymentKey", paymentKey)
                        .param("amount", String.valueOf(amount)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderId))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.SUCCESS.getDescription())); // DB 저장값 확인
    }

    @Test
    @DisplayName("통합 테스트: 결제 취소 성공")
    @WithMockUser
    void cancelPayment_Integration_Success() throws Exception {
        // given: DB에 결제 완료 데이터 저장
        String orderNumber = "B20000000001";
        PaymentCreateRequest createReq = new PaymentCreateRequest(
                "payment_key_cancel", orderNumber, 15000, "CARD", "TOSS", "DONE", LocalDateTime.now(), "url", 0
        );
        paymentRepository.save(new Payment(createReq));

        CommonCancelRequest req = new CommonCancelRequest("payment_key_cancel", 15000, "단순 변심");

        // OrderService 소유자 검증 통과
        given(orderService.existsOrder(orderNumber, 1L)).willReturn(true);

        // Toss API 취소 응답 Mocking
        Cancel cancelInfo = new Cancel(15000, "단순 변심", LocalDateTime.now());
        TossCancelResponse tossCancelResponse = new TossCancelResponse(
                "payment_key_cancel", "CANCELED", List.of(cancelInfo)
        );
        given(tossPaymentsApiClient.cancelPayment(anyString(), anyString(), any(TossCancelRequest.class)))
                .willReturn(tossCancelResponse);

        // when
        mockMvc.perform(post("/payment/cancel")
                        .header("X-USER-ID", 1L)
                        .param("orderNumber", orderNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cancelAmount").value(15000))
                .andExpect(jsonPath("$[0].cancelReason").value("단순 변심"));
    }
}