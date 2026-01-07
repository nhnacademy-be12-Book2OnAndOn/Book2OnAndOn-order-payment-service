package com.nhnacademy.book2onandon_order_payment_service.order.refund.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.order.controller.RefundController;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.RefundService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RefundController.class)
@AutoConfigureMockMvc(addFilters = false) // 보안 필터로 401/403 흔들리는 것 방지 (컨트롤러 테스트에 집중)
class RefundControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean RefundService refundService;

    @Test
    @DisplayName("반품 신청 폼 조회: GET /orders/{orderId}/refunds/form - 회원 헤더로 조회 성공")
    void getRefundForm_member_success() throws Exception {
        long orderId = 10L;
        long userId = 1L;

        given(refundService.getRefundableItems(eq(orderId), eq(userId), isNull()))
                .willReturn(List.of());

        mockMvc.perform(get("/orders/{orderId}/refunds/form", orderId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());

        verify(refundService).getRefundableItems(orderId, userId, null);
    }

    @Test
    @DisplayName("반품 신청 폼 조회: GET /orders/{orderId}/refunds/form - 비회원 토큰으로 조회 성공")
    void getRefundForm_guest_success() throws Exception {
        long orderId = 10L;
        String guestToken = "guest-token";

        given(refundService.getRefundableItems(eq(orderId), isNull(), eq(guestToken)))
                .willReturn(List.of());

        mockMvc.perform(get("/orders/{orderId}/refunds/form", orderId)
                        .header("X-Guest-Order-Token", guestToken))
                .andExpect(status().isOk());

        verify(refundService).getRefundableItems(orderId, null, guestToken);
    }

    @Test
    @DisplayName("반품 신청: POST /orders/{orderId}/refunds - 생성 성공이면 201 반환")
    void createRefund_success_returns201() throws Exception {
        long orderId = 10L;
        Long userId = 1L;
        String guestToken = null;

        String validJson = """
                {
                   "refundReason": "CHANGE_OF_MIND",
                   "refundReasonDetail": "단순 변심",
                   "refundItems": [
                     {
                       "orderItemId": 1,
                       "refundQuantity": 1
                     }
                   ]
                 }
            """;

        RefundResponseDto response = org.mockito.Mockito.mock(RefundResponseDto.class);
        given(refundService.createRefund(eq(orderId), eq(userId), any(RefundRequestDto.class), isNull()))
                .willReturn(response);

        mockMvc.perform(post("/orders/{orderId}/refunds", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson)
                        .header("X-User-Id", userId))
                .andExpect(status().isCreated());

        // request 바인딩이 제대로 되었고, 서비스로 넘어갔는지까지 확인
        verify(refundService).createRefund(eq(orderId), eq(userId), any(RefundRequestDto.class), isNull());
    }

    @Test
    @DisplayName("반품 신청: POST /orders/{orderId}/refunds - @Valid 실패면 400 반환")
    void createRefund_invalid_returns400() throws Exception {
        long orderId = 10L;

        // 보통 빈 JSON은 @Valid에 걸려 400이 나옴 (RefundRequestDto 제약조건에 따라 달라질 수 있음).
        mockMvc.perform(post("/orders/{orderId}/refunds", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("반품 신청 취소: POST /orders/{orderId}/refunds/{refundId}/cancel - 성공")
    void cancelRefund_success() throws Exception {
        long orderId = 10L;
        long refundId = 99L;
        Long userId = 1L;

        RefundResponseDto response = org.mockito.Mockito.mock(RefundResponseDto.class);
        given(refundService.cancelRefund(eq(orderId), eq(refundId), eq(userId), isNull()))
                .willReturn(response);

        mockMvc.perform(post("/orders/{orderId}/refunds/{refundId}/cancel", orderId, refundId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());

        verify(refundService).cancelRefund(orderId, refundId, userId, null);
    }

    @Test
    @DisplayName("반품 상세 조회: GET /orders/{orderId}/refunds/{refundId} - 성공")
    void getRefundDetails_success() throws Exception {
        long orderId = 10L;
        long refundId = 99L;
        Long userId = 1L;
        String guestToken = null;

        RefundResponseDto response = org.mockito.Mockito.mock(RefundResponseDto.class);
        given(refundService.getRefundDetails(eq(orderId), eq(refundId), eq(userId), isNull()))
                .willReturn(response);

        mockMvc.perform(get("/orders/{orderId}/refunds/{refundId}", orderId, refundId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());

        verify(refundService).getRefundDetails(orderId, refundId, userId, guestToken);
    }

    @Test
    @DisplayName("회원 전체 반품 목록 조회: GET /orders/refunds/my-list - 성공 (X-User-Id 필수)")
    void getMyRefunds_success() throws Exception {
        long userId = 1L;

        Page<RefundResponseDto> page = new PageImpl<>(List.of());
        given(refundService.getRefundsForMember(eq(userId), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/orders/refunds/my-list")
                        .header("X-User-Id", userId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());

        verify(refundService).getRefundsForMember(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("회원 전체 반품 목록 조회: GET /orders/refunds/my-list - X-User-Id 누락이면 400")
    void getMyRefunds_missingHeader_returns400() throws Exception {
        mockMvc.perform(get("/orders/refunds/my-list")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isBadRequest());
    }
}
