package com.nhnacademy.book2onandon_order_payment_service.order.order.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.order.controller.OrderGuestController;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderPrepareRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestLoginRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestLoginResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.GuestOrderService;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderService;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OrderGuestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @Mock
    private GuestOrderService guestOrderService;

    @InjectMocks
    private OrderGuestController orderGuestController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl = "/guest/orders";
    private final String guestIdHeader = "X-Guest-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderGuestController).build();
    }

    @Test
    @DisplayName("비회원 로그인 성공 ")
    void loginGuest_Success() throws Exception {
        GuestLoginRequestDto request = new GuestLoginRequestDto("ORD-001", "password");

        GuestLoginResponseDto response = new GuestLoginResponseDto("valid-token", "Bearer");

        given(guestOrderService.loginGuest(any())).willReturn(response);

        mockMvc.perform(post(baseUrl + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("비회원 주문 준비 성공 ")
    void prepareGuestOrder_Success() throws Exception {
        OrderPrepareRequestDto request = new OrderPrepareRequestDto(new ArrayList<>());
        OrderPrepareResponseDto response = OrderPrepareResponseDto.forGuest(new ArrayList<>());

        given(orderService.prepareGuestOrder(anyString(), any())).willReturn(response);

        mockMvc.perform(post(baseUrl + "/prepare")
                        .header(guestIdHeader, "guest-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 주문 생성 성공 ")
    void createGuestOrder_Success() throws Exception {
        OrderCreateResponseDto response = mock(OrderCreateResponseDto.class);
        given(response.getOrderNumber()).willReturn("ORD-GUEST-001");

        given(orderService.createGuestPreOrder(anyString(), any())).willReturn(response);

        mockMvc.perform(post(baseUrl)
                        .header(guestIdHeader, "guest-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestName\":\"홍길동\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-GUEST-001"));
    }

    @Test
    @DisplayName("비회원 주문 취소 성공 ")
    void cancelOrder_Success() throws Exception {
        doNothing().when(orderService).cancelGuestOrder(anyString(), anyString());

        mockMvc.perform(patch(baseUrl + "/ORD-001/cancel")
                        .header("X-Guest-Order-Token", "valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("비회원 주문 취소 실패 - 토큰 누락 ")
    void cancelOrder_Fail_NoToken() {
        assertThatThrownBy(() -> mockMvc.perform(patch(baseUrl + "/ORD-001/cancel")))
                .hasCauseInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("비회원 주문 준비 실패 - 헤더 누락 ")
    void prepareGuestOrder_Fail_NoHeader() throws Exception {
        mockMvc.perform(post(baseUrl + "/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}