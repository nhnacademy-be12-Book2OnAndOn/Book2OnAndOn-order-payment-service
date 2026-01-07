package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.DeliveryPolicyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DeliveryPolicyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeliveryPolicyService deliveryPolicyService;

    @InjectMocks
    private DeliveryPolicyController deliveryPolicyController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String BASE_URL = "/admin/delivery-policies";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deliveryPolicyController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("배송 정책 목록 조회 성공 ")
    void getDeliveryPolicies_Success() throws Exception {
        DeliveryPolicyResponseDto response = new DeliveryPolicyResponseDto(1L, "기본", 3000, 30000);
        PageImpl<DeliveryPolicyResponseDto> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        given(deliveryPolicyService.getPolicies(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deliveryPolicyId").value(1L));
    }

    @Test
    @DisplayName("단일 배송 정책 조회 성공 ")
    void getDeliveryPolicy_Success() throws Exception {
        DeliveryPolicyResponseDto response = new DeliveryPolicyResponseDto(1L, "기본", 3000, 30000);

        given(deliveryPolicyService.getPolicy(1L)).willReturn(response);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryPolicyName").value("기본"));
    }

    @Test
    @DisplayName("배송 정책 생성 성공 ")
    void createPolicy_Success() throws Exception {
        DeliveryPolicyRequestDto request = new DeliveryPolicyRequestDto("신규정책", 2500, 50000);

        doNothing().when(deliveryPolicyService).createPolicy(any(DeliveryPolicyRequestDto.class));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("배송 정책 수정 성공 ")
    void updateDeliveryPolicy_Success() throws Exception {
        DeliveryPolicyRequestDto request = new DeliveryPolicyRequestDto("수정정책", 2000, 40000);

        doNothing().when(deliveryPolicyService).updatePolicy(eq(1L), any(DeliveryPolicyRequestDto.class));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 정책 조회 실패 (Fail Path)")
    void getDeliveryPolicy_NotFound() {
        given(deliveryPolicyService.getPolicy(99L)).willThrow(new RuntimeException("Not Found"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        mockMvc.perform(get(BASE_URL + "/99")))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not Found");
    }

    @Test
    @DisplayName("유효하지 않은 데이터로 정책 생성 실패 ")
    void createPolicy_Fail_InvalidRequest() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryPolicyName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}