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
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.RefundGuestController;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundGuestRequestDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundResponseDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
//import java.util.ArrayList;
//import java.util.List;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//@WebMvcTest(RefundGuestController.class)
//class RefundGuestControllerTest {
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
//    @DisplayName("비회원 반품 신청 성공")
//    @WithMockUser
//    void createRefundForGuest_Success() throws Exception {
//        Long orderId = 1L;
//        RefundGuestRequestDto requestDto = new RefundGuestRequestDto(
//                orderId,
//                new ArrayList<>(),
//                "DEFECT",
//                "제품 파손",
//                "password123!",
//                "비회원고객",
//                "010-1234-5678"
//        );
//
//        RefundResponseDto responseDto = mock(RefundResponseDto.class);
//        given(responseDto.getRefundId()).willReturn(10L);
//
//        given(refundService.createRefundForGuest(eq(orderId), any(RefundGuestRequestDto.class)))
//                .willReturn(responseDto);
//
//        mockMvc.perform(post("/guest/orders/{orderId}/refunds", orderId)
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.refundId").value(10L));
//    }
//
//    @Test
//    @DisplayName("비회원 반품 신청 취소 성공")
//    @WithMockUser
//    void cancelRefundForGuest_Success() throws Exception {
//        Long orderId = 1L;
//        Long refundId = 10L;
//        String password = "password123!";
//        RefundResponseDto responseDto = mock(RefundResponseDto.class);
//        given(responseDto.getRefundStatus()).willReturn("CANCELLED");
//
//        given(refundService.cancelRefundForGuest(orderId, refundId, password)).willReturn(responseDto);
//
//        mockMvc.perform(post("/guest/orders/{orderId}/refunds/{refundId}/cancel", orderId, refundId)
//                        .param("guestPassword", password)
//                        .with(csrf()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.refundStatus").value("CANCELLED"));
//    }
//
//    @Test
//    @DisplayName("비회원 반품 상세 조회 성공")
//    @WithMockUser
//    void getRefundDetailsForGuest_Success() throws Exception {
//        Long orderId = 1L;
//        Long refundId = 10L;
//        RefundResponseDto responseDto = mock(RefundResponseDto.class);
//        given(responseDto.getRefundId()).willReturn(refundId);
//
//        given(refundService.getRefundDetailsForGuest(orderId, refundId)).willReturn(responseDto);
//
//        mockMvc.perform(get("/guest/orders/{orderId}/refunds/{refundId}", orderId, refundId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.refundId").value(refundId));
//    }
//
//    @Test
//    @DisplayName("비회원 반품 신청 폼 조회 성공")
//    @WithMockUser
//    void getRefundFormForGuest_Success() throws Exception {
//        Long orderId = 1L;
//        RefundAvailableItemResponseDto item = new RefundAvailableItemResponseDto(
//                1L, 100L, "비회원 도서", 1, 0, 1, false, true
//        );
//
//        given(refundService.getRefundableItemsForGuest(orderId)).willReturn(List.of(item));
//
//        mockMvc.perform(get("/guest/orders/{orderId}/refunds/form", orderId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].bookTitle").value("비회원 도서"))
//                .andExpect(jsonPath("$[0].refundable").value(true));
//    }
//}