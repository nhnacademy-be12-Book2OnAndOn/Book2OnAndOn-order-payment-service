package com.nhnacademy.book2onandon_order_payment_service.order.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.order.controller.OrderGuestController;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderPrepareRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestLoginRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestLoginResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestOrderCreateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.GuestOrderService;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderGuestController.class)
class OrderGuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private GuestOrderService guestOrderService;

    private static final String GUEST_ID_HEADER = "X-Guest-Id";

    @Test
    @DisplayName("비회원 로그인 성공")
    @WithMockUser // Spring Security 통과용
    void loginGuest_Success() throws Exception {
        // given
        GuestLoginRequestDto requestDto = new GuestLoginRequestDto("ORD-001", "1234");
        GuestLoginResponseDto responseDto = new GuestLoginResponseDto("access-token", "ORD-001");

        given(guestOrderService.loginGuest(any(GuestLoginRequestDto.class)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/guest/orders/login")
                        .with(csrf()) // CSRF 토큰
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.orderNumber").value("ORD-001"))
                .andDo(print());
    }

    @Test
    @DisplayName("비회원 주문 준비 데이터 조회 성공")
    @WithMockUser
    void getGuestOrderPrepare_Success() throws Exception {
        // given
        String guestId = "guest-session-id";
        OrderPrepareRequestDto requestDto = new OrderPrepareRequestDto(List.of());

        OrderPrepareResponseDto responseDto = new OrderPrepareResponseDto(null, null, null, null, null);

        given(orderService.prepareGuestOrder(eq(guestId), any(OrderPrepareRequestDto.class)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/guest/orders/prepare")
                        .with(csrf())
                        .header(GUEST_ID_HEADER, guestId) // 헤더 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("비회원 주문 생성 성공")
    @WithMockUser
    void createGuestOrder_Success() throws Exception {
        // given
        String guestId = "guest-session-id";
        GuestOrderCreateRequestDto requestDto = new GuestOrderCreateRequestDto();
        requestDto.setGuestName("홍길동");
        requestDto.setGuestPhoneNumber("010-1234-5678");

        OrderCreateResponseDto responseDto = new OrderCreateResponseDto();
        responseDto.setOrderId(1L);
        responseDto.setOrderNumber("ORD-GUEST-001");

        given(orderService.createGuestPreOrder(eq(guestId), any(GuestOrderCreateRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/guest/orders")
                        .with(csrf())
                        .header(GUEST_ID_HEADER, guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.orderNumber").value("ORD-GUEST-001"))
                .andDo(print());
    }
}