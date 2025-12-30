package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestOrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.util.AesUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderGuestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AesUtils.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "encryption.secret-key=12345678901234567890123456789012"
})
class OrderGuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private static final String GUEST_ID_HEADER = "X-Guest-Id";

    @Test
    @DisplayName("비회원 주문 준비 성공 (Happy Path)")
    void getGuestOrderPrepare_Success() throws Exception {
        String guestId = UUID.randomUUID().toString();
        OrderPrepareRequestDto requestDto = new OrderPrepareRequestDto(new ArrayList<>());
        OrderPrepareResponseDto responseDto = OrderPrepareResponseDto.forGuest(new ArrayList<>());

        given(orderService.prepareGuestOrder(eq(guestId), any(OrderPrepareRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/guest/orders/prepare")
                        .header(GUEST_ID_HEADER, guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.addresses").value(nullValue()));
    }

    @Test
    @DisplayName("비회원 주문 준비 실패 - 헤더 누락 (Fail Path)")
    void getGuestOrderPrepare_Fail_MissingHeader() throws Exception {
        OrderPrepareRequestDto requestDto = new OrderPrepareRequestDto(new ArrayList<>());

        mockMvc.perform(post("/guest/orders/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비회원 주문 생성 성공 (Happy Path)")
    void createGuestOrder_Success() throws Exception {
        String guestId = UUID.randomUUID().toString();
        GuestOrderCreateRequestDto requestDto = new GuestOrderCreateRequestDto();

        OrderCreateResponseDto responseDto = new OrderCreateResponseDto(
                1L, "GUEST-001", "비회원도서", LocalDateTime.now(),
                20000, 3000, 0, 0, 0, 0, 23000,
                LocalDate.now().plusDays(2), new ArrayList<>(), null
        );

        given(orderService.createGuestPreOrder(eq(guestId), any(GuestOrderCreateRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/guest/orders")
                        .header(GUEST_ID_HEADER, guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("GUEST-001"))
                .andExpect(jsonPath("$.totalAmount").value(23000));
    }

    @Test
    @DisplayName("비회원 주문 상세 조회 (커버리지용 - 현재 null 반환)")
    void findGuestOrderDetails_ReturnsNull() throws Exception {
        mockMvc.perform(get("/guest/orders"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("비회원 주문 취소 (커버리지용 - 현재 null 반환)")
    void cancelGuestOrder_ReturnsNull() throws Exception {
        mockMvc.perform(patch("/guest/orders/{orderId}", 1L)
                        .param("password", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}