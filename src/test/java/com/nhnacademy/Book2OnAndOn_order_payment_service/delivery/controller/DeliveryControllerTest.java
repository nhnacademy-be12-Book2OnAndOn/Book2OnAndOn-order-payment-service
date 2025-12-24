package com.nhnacademy.Book2OnAndOn_order_payment_service.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.DeliveryController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryWaybillUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.DeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DeliveryController.class, properties = {
        "spring.cloud.config.enabled=false",      // Config Server 끄기
})
@WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryService deliveryService;

    @Test
    @DisplayName("사용자 주문 배송 정보 조회 성공")
    void getDeliveryByOrder_success() throws Exception {
        // Given
        Long orderId = 1L;
        Long userId = 100L;

        DeliveryResponseDto responseDto = new DeliveryResponseDto(
                1L, orderId, "SHIPPING", "우체국택배", "123456789", LocalDateTime.now(), "http://tracking-url.com"
        );

        given(deliveryService.getDelivery(eq(orderId), eq(userId), isNull()))
                .willReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/deliveries")
                        .param("orderId", String.valueOf(orderId))
                        .header("X-User-Id", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(1L))
                .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    @DisplayName("관리자 배송 목록 조회 성공 (페이징)")
    void getDeliveries_success() throws Exception {
        // Given
        DeliveryResponseDto dto1 = new DeliveryResponseDto(1L, 10L, "PAYMENT_COMPLETED", null, null, null, null);
        Page<DeliveryResponseDto> responsePage = new PageImpl<>(List.of(dto1));

        given(deliveryService.getDeliveries(any(Pageable.class), any()))
                .willReturn(responsePage);

        // When & Then
        mockMvc.perform(get("/admin/deliveries")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(10L));
    }

    @Test
    @DisplayName("관리자 운송장 번호 등록 성공")
    void registerWaybill_success() throws Exception {
        // Given
        Long deliveryId = 1L;
        DeliveryWaybillUpdateDto requestDto = new DeliveryWaybillUpdateDto("CJ대한통운", "987654321");

        // When & Then
        mockMvc.perform(put("/admin/deliveries/{deliveryId}/waybill", deliveryId)
                        .with(csrf()) // PUT 요청 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(deliveryService).registerWaybill(eq(deliveryId), any(DeliveryWaybillUpdateDto.class));
    }

    @Test
    @DisplayName("관리자 배송 정보(택배사, 운송장) 수정 성공")
    void updateDeliveryInfo_success() throws Exception {
        // Given
        Long deliveryId = 1L;
        DeliveryWaybillUpdateDto requestDto = new DeliveryWaybillUpdateDto("롯데택배", "555555555");

        // When & Then
        mockMvc.perform(put("/admin/deliveries/{deliveryId}/info", deliveryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(deliveryService).updateDeliveryInfo(eq(deliveryId), any(DeliveryWaybillUpdateDto.class));
    }

    @Test
    @DisplayName("운송장 등록 실패 - 유효성 검증(운송장 번호 누락)")
    void registerWaybill_validation_fail() throws Exception {
        // Given
        Long deliveryId = 1L;

        DeliveryWaybillUpdateDto invalidRequest = new DeliveryWaybillUpdateDto("CJ대한통운", null);

        // When & Then
        mockMvc.perform(put("/admin/deliveries/{deliveryId}/waybill", deliveryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400 Bad Request 기대
    }
}