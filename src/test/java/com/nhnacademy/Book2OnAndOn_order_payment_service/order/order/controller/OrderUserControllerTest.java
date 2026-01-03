package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.OrderUserController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:"
})
class OrderUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String GUEST_TOKEN_HEADER = "X-Guest-Order-Token";

    @Test
    @DisplayName("주문 준비 데이터 조회 성공 (Happy Path)")
    void getOrderPrepare_Success() throws Exception {
        Long userId = 1L;
        OrderPrepareRequestDto requestDto = new OrderPrepareRequestDto(new ArrayList<>());
        OrderPrepareResponseDto responseDto = OrderPrepareResponseDto.forMember(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);

        given(orderService.prepareOrder(eq(userId), eq(null), any(OrderPrepareRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/orders/prepare")
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderItems").isArray());
    }

    @Test
    @DisplayName("사전 주문 생성 성공 (Happy Path)")
    void createPreOrder_Success() throws Exception {
        Long userId = 1L;
        OrderCreateRequestDto requestDto = new OrderCreateRequestDto(new ArrayList<>(), null, 1L, LocalDate.now().plusDays(1), null, 0);
        OrderCreateResponseDto responseDto = new OrderCreateResponseDto(
                1L, "ORD-100", "테스트주문", LocalDateTime.now(), 50000, 3000, 0, 0, 0, 0, 53000, LocalDate.now(), new ArrayList<>(), null
        );

        given(orderService.createPreOrder(eq(userId), eq(null), any(OrderCreateRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/orders")
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-100"));
    }

    @Test
    @DisplayName("나의 주문 리스트 조회 성공 (Happy Path)")
    void getOrderList_Success() throws Exception {
        Long userId = 1L;
        OrderSimpleDto dto = new OrderSimpleDto(1L, "ORD-100", OrderStatus.COMPLETED, LocalDateTime.now(), 10000, "제목");
        PageImpl<OrderSimpleDto> page = new PageImpl<>(List.of(dto));

        given(orderService.getOrderList(eq(userId), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/orders/my-order").header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-100"));
    }

    @Test
    @DisplayName("회원 주문 상세 조회 성공 (Happy Path)")
    void getOrderDetail_Member_Success() throws Exception {
        // given
        Long userId = 1L;
        String orderNumber = "ORD-100";
        OrderDetailResponseDto responseDto = new OrderDetailResponseDto();

        // 회원이므로 guestToken은 null이어야 함
        given(orderService.getOrderDetail(eq(userId), eq(orderNumber), isNull()))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/orders/{orderNumber}", orderNumber)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 주문 상세 조회 성공 (Happy Path)")
    void getOrderDetail_Guest_Success() throws Exception {
        // given
        String guestToken = "guest-token-sample";
        String orderNumber = "ORD-100";
        OrderDetailResponseDto responseDto = new OrderDetailResponseDto();

        // 비회원이므로 userId는 null이어야 함
        given(orderService.getOrderDetail(isNull(), eq(orderNumber), eq(guestToken)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/orders/{orderNumber}", orderNumber)
                        .header(GUEST_TOKEN_HEADER, guestToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("주문 취소 성공 (Happy Path)")
    void cancelOrder_Success() throws Exception {
        Long userId = 1L;
        String orderNumber = "ORD-100";

        mockMvc.perform(patch("/orders/{orderNumber}/cancel", orderNumber).header(USER_ID_HEADER, userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("주문 리스트 조회 실패 - 인증 헤더 누락 (Fail Path)")
    void getOrderList_Fail_MissingHeader() throws Exception {
        mockMvc.perform(get("/orders/my-order"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 주문 상세 조회 실패 (Fail Path - 회원)")
    void getOrderDetail_Fail_NotFound() throws Exception {
        // given
        Long userId = 1L;
        String invalidOrderNumber = "INVALID";

        // Service가 예외를 던지도록 설정
        given(orderService.getOrderDetail(eq(userId), eq(invalidOrderNumber), isNull()))
                .willThrow(new IllegalArgumentException("Order not found"));

        // when & then
        mockMvc.perform(get("/orders/{orderNumber}", invalidOrderNumber)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Order not found"));
    }
}