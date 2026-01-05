package com.nhnacademy.Book2OnAndOn_order_payment_service.order.refund.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.config.SecurityConfig;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.RefundController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RefundController.class)
@Import(SecurityConfig.class)
class RefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RefundService refundService;

    @Test
    @DisplayName("반품 신청 폼 조회 - 성공 (회원)")
    @WithMockUser
    void getRefundForm_Member_Success() throws Exception {
        Long orderId = 1L;
        Long userId = 10L;

        RefundAvailableItemResponseDto itemDto = mock(RefundAvailableItemResponseDto.class);
        given(refundService.getRefundableItems(eq(orderId), eq(userId), isNull()))
                .willReturn(List.of(itemDto));

        mockMvc.perform(get("/orders/{orderId}/refunds/form", orderId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("반품 신청 - 성공")
    @WithMockUser
    void createRefund_Success() throws Exception {
        Long orderId = 1L;
        Long userId = 10L;
        RefundRequestDto requestDto = new RefundRequestDto(1L, List.of(), "단순 변심", ""); // 생성자 확인 필요

        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundId()).willReturn(100L);

        given(refundService.createRefund(eq(orderId), eq(userId), any(RefundRequestDto.class), isNull()))
                .willReturn(responseDto);

        mockMvc.perform(post("/orders/{orderId}/refunds", orderId)
                        .with(csrf())
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refundId").value(100L));
    }

    @Test
    @DisplayName("반품 신청 취소 - 성공")
    @WithMockUser
    void cancelRefund_Success() throws Exception {
        Long orderId = 1L;
        Long refundId = 100L;
        Long userId = 10L;

        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(refundService.cancelRefund(orderId, refundId, userId, null))
                .willReturn(responseDto);

        mockMvc.perform(post("/orders/{orderId}/refunds/{refundId}/cancel", orderId, refundId)
                        .with(csrf())
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("반품 상세 조회 - 성공 (게스트)")
    @WithMockUser
    void getRefundDetails_Guest_Success() throws Exception {
        Long orderId = 1L;
        Long refundId = 100L;
        String guestToken = "guest-token";

        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundId()).willReturn(refundId);

        given(refundService.getRefundDetails(orderId, refundId, null, guestToken))
                .willReturn(responseDto);

        mockMvc.perform(get("/orders/{orderId}/refunds/{refundId}", orderId, refundId)
                        .header("X-Guest-Order-Token", guestToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(refundId));
    }

    @Test
    @DisplayName("내 반품 목록 조회 - 성공")
    @WithMockUser
    void getMyRefunds_Success() throws Exception {
        Long userId = 10L;
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        PageImpl<RefundResponseDto> page = new PageImpl<>(List.of(responseDto));

        given(refundService.getRefundsForMember(eq(userId), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/orders/refunds/my-list")
                        .header("X-User-Id", userId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}