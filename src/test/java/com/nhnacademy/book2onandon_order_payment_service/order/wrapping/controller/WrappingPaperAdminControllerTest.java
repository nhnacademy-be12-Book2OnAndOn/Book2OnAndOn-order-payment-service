package com.nhnacademy.book2onandon_order_payment_service.order.wrapping.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.order.controller.WrappingPaperAdminController;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.WrappingPaperService;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class WrappingPaperAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WrappingPaperService wrappingPaperService;

    @InjectMocks
    private WrappingPaperAdminController wrappingPaperAdminController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(wrappingPaperAdminController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator) // 생성한 Validator를 등록합니다.
                .build();
    }

    @Test
    @DisplayName("포장지 등록 성공 ")
    void createWrappingPaper_Success() throws Exception {
        WrappingPaperRequestDto request = new WrappingPaperRequestDto("선물용", 1000, "path/image.png");

        WrappingPaperResponseDto actualResponse = new WrappingPaperResponseDto(
                1L,
                "선물용",
                1000,
                "path/image.png"
        );

        given(wrappingPaperService.createWrappingPaper(any(WrappingPaperRequestDto.class)))
                .willReturn(actualResponse);

        mockMvc.perform(post("/admin/wrappapers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wrappingPaperId").value(1L))
                .andExpect(jsonPath("$.wrappingPaperName").value("선물용"))
                .andExpect(jsonPath("$.wrappingPaperPrice").value(1000));
    }

    @Test
    @DisplayName("전체 포장지 목록 조회 성공 ")
    void getAllWrappingPapers_Success() throws Exception {
        WrappingPaperResponseDto response = new WrappingPaperResponseDto(1L, "기본", 500, "path");
        PageImpl<WrappingPaperResponseDto> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        given(wrappingPaperService.getAllWrappingPapers(any())).willReturn(page);

        mockMvc.perform(get("/admin/wrappapers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].wrappingPaperName").value("기본"));
    }

    @Test
    @DisplayName("단건 포장지 조회 실패 - 리소스 없음 ")
    void getWrappingPaper_Fail_NotFound() {
        given(wrappingPaperService.getWrappingPaper(anyLong())).willThrow(new RuntimeException("Not Found"));

        assertThatThrownBy(() -> mockMvc.perform(get("/admin/wrappapers/999")))
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("포장지 정보 수정 실패 - 유효성 검사 실패 ")
    void updateWrappingPaper_Fail_Validation() throws Exception {
        String invalidJson = "{\"wrappingPaperName\": \"\", \"wrappingPaperPrice\": -1, \"wrappingPaperImagePath\": \"\"}";

        mockMvc.perform(put("/admin/wrappapers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포장지 삭제 실패 - 서버 오류 발생 ")
    void deleteWrappingPaper_Fail_ServerError() {
        doThrow(new RuntimeException("DB Error")).when(wrappingPaperService).deleteWrappingPaper(anyLong());

        assertThatThrownBy(() -> mockMvc.perform(delete("/admin/wrappapers/1")))
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("단건 포장지 조회 성공 ")
    void getWrappingPaper_Success() throws Exception {
        WrappingPaperResponseDto response = new WrappingPaperResponseDto(1L, "고급", 2000, "path");
        given(wrappingPaperService.getWrappingPaper(1L)).willReturn(response);

        mockMvc.perform(get("/admin/wrappapers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wrappingPaperName").value("고급"));
    }

    @Test
    @DisplayName("포장지 삭제 성공 ")
    void deleteWrappingPaper_Success() throws Exception {
        doNothing().when(wrappingPaperService).deleteWrappingPaper(1L);

        mockMvc.perform(delete("/admin/wrappapers/1"))
                .andExpect(status().isNoContent());
    }
}