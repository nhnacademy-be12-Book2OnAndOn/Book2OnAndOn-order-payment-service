package com.nhnacademy.Book2OnAndOn_order_payment_service.order.deliveryPolicy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.DeliveryPolicyController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.DeliveryPolicyNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.DeliveryPolicyService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeliveryPolicyController.class)
@WithMockUser(username = "admin", roles = {"ADMIN"}) // 관리자 권한 부여
class DeliveryPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryPolicyService deliveryPolicyService;

    @Test
    @DisplayName("배송 정책 목록 조회 API")
    void getDeliveryPolicies_success() throws Exception {
        // Given
        DeliveryPolicyResponseDto dto = new DeliveryPolicyResponseDto(1L, "기본", 3000, 50000);
        Page<DeliveryPolicyResponseDto> responsePage = new PageImpl<>(List.of(dto));

        given(deliveryPolicyService.getPolicies(any(Pageable.class))).willReturn(responsePage);

        // When & Then
        mockMvc.perform(get("/admin/delivery-policies")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deliveryPolicyName").value("기본"));
    }

    @Test
    @DisplayName("배송 정책 단건 조회 API")
    void getDeliveryPolicy_success() throws Exception {
        // Given
        Long policyId = 1L;
        DeliveryPolicyResponseDto dto = new DeliveryPolicyResponseDto(policyId, "기본", 3000, 50000);

        given(deliveryPolicyService.getPolicy(policyId)).willReturn(dto);

        // When & Then
        mockMvc.perform(get("/admin/delivery-policies/{id}", policyId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryPolicyId").value(policyId));
    }

    @Test
    @DisplayName("배송 정책 생성 API")
    void createPolicy_success() throws Exception {
        // Given
        DeliveryPolicyRequestDto requestDto = new DeliveryPolicyRequestDto("신규 정책", 3000, 50000);

        // When & Then
        mockMvc.perform(post("/admin/delivery-policies")
                        .with(csrf()) // POST 요청 시 CSRF 토큰 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated()); // 201 Created

        verify(deliveryPolicyService).createPolicy(any(DeliveryPolicyRequestDto.class));
    }

    @Test
    @DisplayName("배송 정책 수정 API")
    void updateDeliveryPolicy_success() throws Exception {
        // Given
        Long policyId = 1L;
        DeliveryPolicyRequestDto requestDto = new DeliveryPolicyRequestDto("수정 정책", 4000, 60000);

        // When & Then
        mockMvc.perform(put("/admin/delivery-policies/{id}", policyId)
                        .with(csrf()) // PUT 요청 시 CSRF 토큰 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNoContent()); // 204 No Content

        verify(deliveryPolicyService).updatePolicy(eq(policyId), any(DeliveryPolicyRequestDto.class));
    }

    @Test
    @DisplayName("정책 단건 조회 실패 - 404 Not Found (존재하지 않는 ID)")
    void getDeliveryPolicy_fail_notFound() throws Exception {
        // Given
        Long invalidId = 999L;
        // Service가 예외를 던지도록 Stubbing
        given(deliveryPolicyService.getPolicy(invalidId))
                .willThrow(new DeliveryPolicyNotFoundException(invalidId));

        // When & Then
        mockMvc.perform(get("/admin/delivery-policies/{id}", invalidId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                // ExceptionHandler가 설정되어 있다면 404, 없다면 예외 종류에 따라 500 등이 나올 수 있음
                // 일반적으로 @ControllerAdvice로 404를 반환하도록 설정했다고 가정
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("정책 수정 실패 - 404 Not Found (존재하지 않는 ID)")
    void updateDeliveryPolicy_fail_notFound() throws Exception {
        // Given
        Long invalidId = 999L;
        DeliveryPolicyRequestDto requestDto = new DeliveryPolicyRequestDto("수정", 3000, 50000);

        // void 메서드에서 예외 던지기 Stubbing
        willThrow(new DeliveryPolicyNotFoundException(invalidId))
                .given(deliveryPolicyService).updatePolicy(eq(invalidId), any(DeliveryPolicyRequestDto.class));

        // When & Then
        mockMvc.perform(put("/admin/delivery-policies/{id}", invalidId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("정책 생성 실패 - 400 Bad Request (유효성 검사 실패)")
    void createPolicy_fail_validation() throws Exception {
        // Given
        // 이름이 null이고 금액이 음수인 잘못된 요청 데이터
        DeliveryPolicyRequestDto invalidDto = new DeliveryPolicyRequestDto(null, -1000, -500);

        // When & Then
        mockMvc.perform(post("/admin/delivery-policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400 에러 기대
    }
}