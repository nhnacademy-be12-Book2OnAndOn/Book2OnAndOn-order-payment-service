//package com.nhnacademy.Book2OnAndOn_order_payment_service.order.refund.controller;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.mock;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.RefundUserController;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundRequestDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundResponseDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
//import java.util.ArrayList;
//import java.util.List;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//@WebMvcTest(RefundUserController.class)
//class RefundUserControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockitoBean
//    private RefundService refundService;
//
//    @Test
//    @DisplayName("회원 반품 신청 성공")
//    @WithMockUser(username = "100")
//    void createRefund_Success() throws Exception {
//        Long orderId = 1L;
//        RefundRequestDto requestDto = new RefundRequestDto(orderId, new ArrayList<>(), "CHANGE_OF_MIND", "단순변심");
//        RefundResponseDto responseDto = mock(RefundResponseDto.class);
//        given(responseDto.getRefundId()).willReturn(50L);
//
//        given(refundService.createRefundForMember(eq(orderId), eq(100L), any(RefundRequestDto.class)))
//                .willReturn(responseDto);
//
//        mockMvc.perform(post("/orders/{orderId}/refunds", orderId)
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.refundId").value(50L));
//    }
//
//    @Test
//    @DisplayName("반품 신청 폼(가능 아이템 조회) 성공")
//    @WithMockUser(username = "100")
//    void getRefundForm_Success() throws Exception {
//        Long orderId = 1L;
//        RefundAvailableItemResponseDto item = new RefundAvailableItemResponseDto(
//                10L, 200L, "테스트 도서", 2, 0, 2, false, true
//        );
//
//        given(refundService.getRefundableItemsForMember(orderId, 100L)).willReturn(List.of(item));
//
//        mockMvc.perform(get("/orders/{orderId}/refunds/form", orderId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].bookTitle").value("테스트 도서"))
//                .andExpect(jsonPath("$[0].refundable").value(true));
//    }
//
//    @Test
//    @DisplayName("회원 반품 신청 취소 성공")
//    @WithMockUser(username = "100")
//    void cancelRefund_Success() throws Exception {
//        Long orderId = 1L;
//        Long refundId = 50L;
//
//        RefundResponseDto responseDto = mock(RefundResponseDto.class);
//
//        given(responseDto.getRefundStatus()).willReturn("CANCELLED");
//        given(responseDto.getRefundId()).willReturn(refundId);
//
//        given(refundService.cancelRefundForMember(orderId, refundId, 100L)).willReturn(responseDto);
//
//        mockMvc.perform(post("/orders/{orderId}/refunds/{refundId}/cancel", orderId, refundId)
//                        .with(csrf()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.refundStatus").value("CANCELLED"))
//                .andExpect(jsonPath("$.refundId").value(50L));
//    }
//
//    @Test
//    @DisplayName("회원 반품 상세 조회 성공")
//    @WithMockUser(username = "100")
//    void getRefundDetails_Success() throws Exception {
//        Long orderId = 1L;
//        Long refundId = 50L;
//        RefundResponseDto responseDto = mock(RefundResponseDto.class);
//        given(responseDto.getRefundId()).willReturn(refundId);
//
//        given(refundService.getRefundDetailsForMember(orderId, refundId, 100L)).willReturn(responseDto);
//
//        mockMvc.perform(get("/orders/{orderId}/refund/{refundId}", orderId, refundId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.refundId").value(refundId));
//    }
//
//    @Test
//    @DisplayName("내 반품 목록 조회 성공")
//    @WithMockUser(username = "100")
//    void getMyRefunds_Success() throws Exception {
//        RefundResponseDto responseDto = mock(RefundResponseDto.class);
//        given(responseDto.getRefundId()).willReturn(50L);
//        PageImpl<RefundResponseDto> page = new PageImpl<>(List.of(responseDto));
//
//        given(refundService.getRefundsForMember(eq(100L), any())).willReturn(page);
//
//        mockMvc.perform(get("/orders/refunds/my-list")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content[0].refundId").value(50L));
//    }
//}