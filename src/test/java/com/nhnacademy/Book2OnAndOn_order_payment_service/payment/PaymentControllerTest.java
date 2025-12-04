package com.nhnacademy.Book2OnAndOn_order_payment_service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.SecurityConfig;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.controller.PaymentController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Spring Boot 3.4+ (or use @MockBean)
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
//@Import(SecurityConfig.class) // Security 설정 적용 (CSRF disable 등)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private PaymentStrategyFactory factory;

    @MockitoBean
    private PaymentStrategy paymentStrategy;

    @Test
    @DisplayName("결제 승인 요청 성공 (GET)")
    @WithMockUser // 인증된 사용자로 테스트
    void successPaymentAndConfirm() throws Exception {
        // given
        String provider = "TOSS";
        String orderId = "B20001";
        String paymentKey = "key_123";
        int amount = 10000;

        // 전략 패턴 Mocking
        given(factory.getStrategy(provider)).willReturn(paymentStrategy);
        given(paymentStrategy.getProvider()).willReturn(provider);

        // 승인 로직 Mocking
        CommonConfirmResponse confirmResp = new CommonConfirmResponse(
                paymentKey, orderId, amount, "CARD", "DONE", LocalDateTime.now(), "http://url"
        );
        given(paymentStrategy.confirmAndProcessPayment(any(CommonConfirmRequest.class)))
                .willReturn(confirmResp);

        // DB 저장 Mocking
        PaymentResponse response = new PaymentResponse(
                paymentKey, orderId, amount, "CARD", provider, "DONE", LocalDateTime.now(), "http://url", 0
        );
        given(paymentService.createPayment(any(PaymentCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(get("/payment/{provider}/confirm", provider)
                        .param("orderId", orderId)
                        .param("paymentKey", paymentKey)
                        .param("amount", String.valueOf(amount))
                        .header("X-USER-ID", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderId))
                .andExpect(jsonPath("$.paymentStatus").value("DONE"));
    }

    @Test
    @DisplayName("결제 취소 요청 성공 (POST)")
    @WithMockUser // 인증된 사용자로 테스트
    void cancelPayment_Success() throws Exception {
        // given
        String orderNumber = "B20001";
        Long userId = 1L;
        CommonCancelRequest req = new CommonCancelRequest("key_123", 10000, "단순 변심");

        // 검증 통과 Mocking
        given(orderService.existsOrder(orderNumber, userId)).willReturn(true);
        given(paymentService.getProvider(orderNumber)).willReturn("TOSS");
        given(factory.getStrategy("TOSS")).willReturn(paymentStrategy);

        // 취소 전략 Mocking
        CommonCancelResponse cancelResp = new CommonCancelResponse("key_123", "CANCELED", List.of());
        given(paymentStrategy.cancelPayment(any(CommonCancelRequest.class), eq(orderNumber)))
                .willReturn(cancelResp);

        // 취소 내역 저장 Mocking
        PaymentCancelResponse responseDto = new PaymentCancelResponse("key_123", 10000, "단순 변심", LocalDateTime.now());
        given(paymentService.createPaymentCancel(any(PaymentCancelCreateRequest.class)))
                .willReturn(List.of(responseDto));

        // when & then
        mockMvc.perform(post("/payment/cancel")
                        .header("X-USER-ID", userId)
                        .param("orderNumber", orderNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cancelAmount").value(10000));
    }
}