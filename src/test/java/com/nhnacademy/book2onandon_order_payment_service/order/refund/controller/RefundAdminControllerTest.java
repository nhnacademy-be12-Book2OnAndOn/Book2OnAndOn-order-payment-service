package com.nhnacademy.book2onandon_order_payment_service.order.refund.controller;

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
import com.nhnacademy.book2onandon_order_payment_service.order.controller.RefundAdminController;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundSearchCondition;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundStatusUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.RefundService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RefundAdminController.class)
@Import({RefundAdminControllerTest.TestSecurityConfig.class})
@EnableMethodSecurity
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class RefundAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean
    RefundService refundService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    // 핵심: 익명 인증을 끄면, 미인증 요청이 "anonymous"로 통과하지 못하고 AuthenticationEntryPoint로 떨어짐
                    .anonymous(AbstractHttpConfigurer::disable)
                    // 핵심: 필터 단계에서 "인증 필요"로 강제
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    // 401을 확실히 보기 위한 가장 단순한 방법(테스트 전용)
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }

    @Test
    @DisplayName("관리자 반품 목록 조회 성공 - 검색 조건 포함")
    @WithMockUser(roles = "ORDER_ADMIN")
    void getRefundList_Success() throws Exception {
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundId()).willReturn(1L);
        PageImpl<RefundResponseDto> page = new PageImpl<>(List.of(responseDto));

        given(refundService.getRefundListForAdmin(any(RefundSearchCondition.class), any(Pageable.class)))
                .willReturn(page);

        // 컨트롤러 매핑: @GetMapping (경로 없음) => GET "/"
        mockMvc.perform(get("/")
                        // RefundSearchCondition이 @ModelAttribute로 바인딩됨
                        .param("status", "0")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31")
                        .param("userId", "1")
                        .param("orderNumber", "2025123456789")
                        .param("includeGuest", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].refundId").value(1L));
    }

    @Test
    @DisplayName("관리자 반품 상세 조회 성공")
    @WithMockUser(roles = "ORDER_ADMIN")
    void findRefundDetails_Success() throws Exception {
        Long refundId = 1L;
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundId()).willReturn(refundId);

        given(refundService.getRefundDetailsForAdmin(refundId)).willReturn(responseDto);

        // 컨트롤러 매핑: GET /admin/refunds/{refundId}
        mockMvc.perform(get("/admin/refunds/{refundId}", refundId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(refundId));
    }

    @Test
    @DisplayName("관리자 반품 상태 변경 성공")
    @WithMockUser(roles = "ORDER_ADMIN")
    void updateRefundStatus_Success() throws Exception {
        Long refundId = 1L;
        RefundStatusUpdateRequestDto requestDto = new RefundStatusUpdateRequestDto(3);

        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        given(responseDto.getRefundStatus()).willReturn("COMPLETED");

        given(refundService.updateRefundStatus(eq(refundId), any(RefundStatusUpdateRequestDto.class)))
                .willReturn(responseDto);

        // 컨트롤러 매핑: PATCH /admin/refunds/{refundId}
        mockMvc.perform(patch("/admin/refunds/{refundId}", refundId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("관리자 전용 API는 일반 유저(ROLE_USER)가 접근하면 403을 반환한다")
    @WithMockUser(roles = "USER")
    void adminAccess_Denied_ForUser() throws Exception {
        // 목록 API는 GET "/" 이고, 클래스에 @PreAuthorize가 걸려있음
        mockMvc.perform(get("/"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 접근하면 401 Unauthorized를 반환한다")
    void adminAccess_Denied_ForGuest() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("관리자 권한(ROLE_ORDER_ADMIN)이 있으면 정상 접근된다")
    @WithMockUser(roles = "ORDER_ADMIN")
    void adminAccess_Granted() throws Exception {
        RefundResponseDto responseDto = mock(RefundResponseDto.class);
        PageImpl<RefundResponseDto> page = new PageImpl<>(List.of(responseDto));

        given(refundService.getRefundListForAdmin(any(RefundSearchCondition.class), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
