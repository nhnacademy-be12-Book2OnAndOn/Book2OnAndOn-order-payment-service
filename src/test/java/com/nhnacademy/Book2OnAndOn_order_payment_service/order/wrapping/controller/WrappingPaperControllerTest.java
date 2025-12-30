package com.nhnacademy.Book2OnAndOn_order_payment_service.order.wrapping.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller.WrappingPaperController;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrappingPaperSimpleResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.WrappingPaperService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = WrappingPaperController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration.class
        }
)
class WrappingPaperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WrappingPaperService wrappingPaperService;

    private WrappingPaperSimpleResponseDto createSimpleDto(Long id, String name, int price) {
        return new WrappingPaperSimpleResponseDto(
            id,
            name,
            price
        );
    }

    @Test
    @DisplayName("GET /wrappapers - 포장지 목록 조회 성공 (200 OK)")
    void getWrappingPaperList_success() throws Exception {

        WrappingPaperSimpleResponseDto dto1 = createSimpleDto(1L, "기본 포장", 1000);
        WrappingPaperSimpleResponseDto dto2 = createSimpleDto(2L, "고급 포장", 3000);
        List<WrappingPaperSimpleResponseDto> list = List.of(dto1, dto2);

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "wrappingPaperId"));
        Page<WrappingPaperSimpleResponseDto> page = new PageImpl<>(list, pageable, 100);

        when(wrappingPaperService.getWrappingPaperList(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/wrappapers")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].wrappingPaperName", is("기본 포장")))
            .andExpect(jsonPath("$.content[0].wrappingPaperPrice", is(1000)))
            .andExpect(jsonPath("$.totalPages", is(5)))
            .andExpect(jsonPath("$.totalElements", is(100)));

        verify(wrappingPaperService, times(1)).getWrappingPaperList(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /wrappapers?size=5&page=1 - 사용자 지정 페이징 조회 성공 (200 OK)")
    void getWrappingPaperList_customPaging_success() throws Exception {

        List<WrappingPaperSimpleResponseDto> list = List.of(
            createSimpleDto(6L, "A", 100),
            createSimpleDto(7L, "B", 100)
        );
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "wrappingPaperId"));
        Page<WrappingPaperSimpleResponseDto> page = new PageImpl<>(list, pageable, 52);

        when(wrappingPaperService.getWrappingPaperList(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/wrappapers")
                .param("page", "1")
                .param("size", "5")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2))) 
            .andExpect(jsonPath("$.number", is(1)))
            .andExpect(jsonPath("$.size", is(5)))
            .andExpect(jsonPath("$.totalElements", is(52)));

        verify(wrappingPaperService, times(1)).getWrappingPaperList(any(Pageable.class));
    }
}