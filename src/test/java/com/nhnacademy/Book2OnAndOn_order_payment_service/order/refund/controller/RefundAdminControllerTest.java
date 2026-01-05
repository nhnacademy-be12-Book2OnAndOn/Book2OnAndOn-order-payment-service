package com.nhnacademy.Book2OnAndOn_order_payment_service.order.refund.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.config.SecurityConfig;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.RefundAdminController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundSearchCondition;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundStatusUpdateRequestDto;
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

@WebMvcTest(RefundAdminController.class)
@Import(SecurityConfig.class) // Security 설정이 있다면 Import
class RefundAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RefundService refundService;

    @Test
    @DisplayName("관리자 반품 목록 조회 - 성공")
    @WithMockUser(roles = "ORDER_ADMIN")
    void getRefundList_Success() throws Exception {
        // Given
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundId()).willReturn(1L);
        PageImpl<RefundResponseDto> page = new PageImpl<>(List.of(responseDto));

        given(refundService.getRefundListForAdmin(any(RefundSearchCondition.class), any(Pageable.class)))
                .willReturn(page);

        // When & Then
        mockMvc.perform(get("/admin/refunds")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].refundId").value(1L));
    }

    @Test
    @DisplayName("관리자 반품 상세 조회 - 성공")
    @WithMockUser(roles = "ORDER_ADMIN")
    void findRefundDetails_Success() throws Exception {
        // Given
        Long refundId = 1L;
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundId()).willReturn(refundId);

        given(refundService.getRefundDetailsForAdmin(refundId)).willReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/admin/refunds/{refundId}", refundId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(refundId));
    }

    @Test
    @DisplayName("관리자 반품 상태 변경 - 성공")
    @WithMockUser(roles = "ORDER_ADMIN")
    void updateRefundStatus_Success() throws Exception {
        // Given
        Long refundId = 1L;
        RefundStatusUpdateRequestDto requestDto = new RefundStatusUpdateRequestDto(1); // 예시 상태값
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundId()).willReturn(refundId);

        given(refundService.updateRefundStatus(eq(refundId), any(RefundStatusUpdateRequestDto.class)))
                .willReturn(responseDto);

        // When & Then
        mockMvc.perform(patch("/admin/refunds/{refundId}", refundId)
                        .with(csrf()) // POST, PATCH 등은 CSRF 토큰 필요 (Security 설정에 따라)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(refundId));
    }

    @Test
    @DisplayName("관리자 권한 없음 - 403 Forbidden")
    @WithMockUser(roles = "USER")
    void accessDenied_ForUser() throws Exception {
        mockMvc.perform(get("/admin/refunds/1"))
                .andExpect(status().isForbidden());
    }
}