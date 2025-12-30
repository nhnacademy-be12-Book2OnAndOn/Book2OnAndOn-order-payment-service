package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
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

    @Test
    @DisplayName("결제 승인 요청 시 성공하면 201 Created와 결제 정보를 반환한다")
    void confirmPayment_Success() throws Exception {
        String provider = "toss";
        CommonConfirmRequest request = new CommonConfirmRequest("order-123", "pay-key-abc", 10000);

        PaymentResponse response = mock(PaymentResponse.class);
        given(response.paymentKey()).willReturn("pay-key-abc");
        given(response.paymentStatus()).willReturn("DONE");

        given(paymentService.confirmAndCreatePayment(eq(provider), any(CommonConfirmRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/payment/{provider}/confirm", provider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentKey").value("pay-key-abc"))
                .andExpect(jsonPath("$.paymentStatus").value("DONE"));
    }
}