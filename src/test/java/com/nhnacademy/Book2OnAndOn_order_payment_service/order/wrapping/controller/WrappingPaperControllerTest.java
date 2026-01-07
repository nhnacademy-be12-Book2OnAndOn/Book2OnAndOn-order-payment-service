package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrappingPaperSimpleResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.WrappingPaperService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class WrappingPaperControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WrappingPaperService wrappingPaperService;

    @InjectMocks
    private WrappingPaperController wrappingPaperController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(wrappingPaperController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("포장지 목록 조회 성공 ")
    void getWrappingPaperList_Success() throws Exception {
        WrappingPaperSimpleResponseDto dto = new WrappingPaperSimpleResponseDto(1L, "기본 포장지", 1000);

        List<WrappingPaperSimpleResponseDto> content = new ArrayList<>();
        content.add(dto);
        PageImpl<WrappingPaperSimpleResponseDto> page = new PageImpl<>(content, PageRequest.of(0, 20), 1);

        given(wrappingPaperService.getWrappingPaperList(any())).willReturn(page);

        mockMvc.perform(get("/wrappapers")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].wrappingPaperId").value(1L));
    }

    @Test
    @DisplayName("포장지 목록 조회 실패 - 서비스 계층 예외 발생 ")
    void getWrappingPaperList_Fail_InternalError() {
        given(wrappingPaperService.getWrappingPaperList(any()))
                .willThrow(new RuntimeException("Database Error"));

        assertThatThrownBy(() -> mockMvc.perform(get("/wrappapers")))
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("빈 포장지 목록 조회 성공 ")
    void getWrappingPaperList_Empty_Success() throws Exception {
        List<WrappingPaperSimpleResponseDto> emptyList = new ArrayList<>();
        Page<WrappingPaperSimpleResponseDto> emptyPage = new PageImpl<>(emptyList, PageRequest.of(0, 20), 0);

        given(wrappingPaperService.getWrappingPaperList(any())).willReturn(emptyPage);

        mockMvc.perform(get("/wrappapers")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}