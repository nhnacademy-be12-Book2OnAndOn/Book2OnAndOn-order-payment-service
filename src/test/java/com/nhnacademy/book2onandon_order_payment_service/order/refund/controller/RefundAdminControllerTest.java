package com.nhnacademy.book2onandon_order_payment_service.order.refund.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.order.controller.RefundAdminController;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundSearchCondition;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundStatusUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.RefundService;
import java.util.ArrayList;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RefundAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RefundService refundService;

    @InjectMocks
    private RefundAdminController refundAdminController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String BASE_URL = "/admin/refunds";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(refundAdminController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("전체 반품 목록 조회 성공 (Happy Path)")
    void getAllRefunds_Success() throws Exception {
        RefundResponseDto responseDto = new RefundResponseDto(
                1L, 100L, "사유", "상세사유", "신청완료", null, new ArrayList<>()
        );
        List<RefundResponseDto> content = new ArrayList<>();
        content.add(responseDto);
        PageImpl<RefundResponseDto> page = new PageImpl<>(content, PageRequest.of(0, 10), 1);

        given(refundService.getRefundListForAdmin(any(RefundSearchCondition.class), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].refundId").value(1L));
    }

    @Test
    @DisplayName("반품 상태 변경 성공 (Happy Path)")
    void updateRefundStatus_Success() throws Exception {
        RefundResponseDto responseDto = new RefundResponseDto(
                1L, 100L, "사유", "상세사유", "반품완료", null, new ArrayList<>()
        );

        given(refundService.updateRefundStatus(eq(1L), any(RefundStatusUpdateRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(patch("/admin/refunds/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusCode\": 2}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundStatus").value("반품완료"));
    }
}