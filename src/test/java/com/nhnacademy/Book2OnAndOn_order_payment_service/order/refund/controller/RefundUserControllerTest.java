package com.nhnacademy.Book2OnAndOn_order_payment_service.order.refund.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.RefundController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RefundUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RefundService refundService;

    @InjectMocks
    private RefundController refundController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String USER_ID_HEADER = "X-User-Id";
    private final String GUEST_TOKEN_HEADER = "X-Guest-Order-Token";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(refundController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("회원 반품 신청 성공 ")
    void createRefund_Member_Success() throws Exception {
        Long orderId = 1L;
        Long userId = 100L;
        RefundRequestDto requestDto = new RefundRequestDto(orderId, new ArrayList<>(), "CHANGE_OF_MIND", "단순변심");
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundId()).willReturn(50L);

        given(refundService.createRefund(eq(orderId), eq(userId), any(RefundRequestDto.class), isNull()))
                .willReturn(responseDto);

        mockMvc.perform(post("/orders/{orderId}/refunds", orderId)
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refundId").value(50L));
    }

    @Test
    @DisplayName("비회원 반품 신청 성공 ")
    void createRefund_Guest_Success() throws Exception {
        Long orderId = 1L;
        String guestToken = "guest-jwt";
        RefundRequestDto requestDto = new RefundRequestDto(orderId, new ArrayList<>(), "CHANGE_OF_MIND", "단순변심");
        RefundResponseDto responseDto = mock(RefundResponseDto.class);

        given(refundService.createRefund(eq(orderId), isNull(), any(RefundRequestDto.class), eq(guestToken)))
                .willReturn(responseDto);

        mockMvc.perform(post("/orders/{orderId}/refunds", orderId)
                        .header(GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("반품 신청 폼 조회 성공 ")
    void getRefundForm_Success() throws Exception {
        Long orderId = 1L;
        RefundAvailableItemResponseDto item = new RefundAvailableItemResponseDto(
                10L, 200L, "테스트 도서", 2, 0, 2, false, true,0
        );

        given(refundService.getRefundableItems(eq(orderId), eq(100L), isNull())).willReturn(List.of(item));

        mockMvc.perform(get("/orders/{orderId}/refunds/form", orderId)
                        .header(USER_ID_HEADER, 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookTitle").value("테스트 도서"));
    }

    @Test
    @DisplayName("반품 신청 취소 성공 ")
    void cancelRefund_Success() throws Exception {
        Long orderId = 1L;
        Long refundId = 50L;
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundStatus()).willReturn("CANCELLED");

        given(refundService.cancelRefund(eq(orderId), eq(refundId), eq(100L), isNull())).willReturn(responseDto);

        mockMvc.perform(post("/orders/{orderId}/refunds/{refundId}/cancel", orderId, refundId)
                        .header(USER_ID_HEADER, 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundStatus").value("CANCELLED"));
    }



    @Test
    @DisplayName("잘못된 형식의 반품 신청 요청 시 400 에러 ")
    void createRefund_Fail_InvalidRequest() throws Exception {
        mockMvc.perform(post("/orders/1/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("반품 상세 조회 - 존재하지 않는 경우 예외 발생")
    void getRefundDetails_Fail_NotFound() {
        given(refundService.getRefundDetails(anyLong(), anyLong(), anyLong(), any()))
                .willThrow(new RuntimeException("Not Found"));

        assertThatThrownBy(() -> mockMvc.perform(get("/orders/1/refunds/999")
                .header(USER_ID_HEADER, 100L)))
                .hasCauseInstanceOf(RuntimeException.class);
    }
}