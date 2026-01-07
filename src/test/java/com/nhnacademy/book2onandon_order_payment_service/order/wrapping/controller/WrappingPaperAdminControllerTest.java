package com.nhnacademy.book2onandon_order_payment_service.order.wrapping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book2onandon_order_payment_service.order.controller.WrappingPaperAdminController;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.WrappingPaperService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WrappingPaperAdminController.class)
@WithMockUser(username = "testAdmin", roles = {"ADMIN"})
class WrappingPaperAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WrappingPaperService wrappingPaperService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/admin/wrappapers";
    private static final Long TEST_WRAPPER_ID = 1L;

    private WrappingPaperResponseDto createResponseDto() {
        return new WrappingPaperResponseDto(
            TEST_WRAPPER_ID, 
            "고급 포장지", 
            5000, 
            "/images/wrap/premium.png"
        );
    }

    // 1. C (Create) - 포장지 등록 테스트
    @Test
    @DisplayName("POST /admin/wrappapers - 포장지 등록 성공 (201 Created)")
    void createWrappingPaper_success() throws Exception {
        // Given
        WrappingPaperRequestDto requestDto = new WrappingPaperRequestDto(
            "친환경 포장지", 
            2000, 
            "/images/wrap/eco.png"
        );
        WrappingPaperResponseDto responseDto = new WrappingPaperResponseDto(
            2L, 
            "친환경 포장지", 
            2000, 
            "/images/wrap/eco.png"
        );

        when(wrappingPaperService.createWrappingPaper(any(WrappingPaperRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(csrf()))
                .andExpect(status().isCreated()) // 201 Created 확인
                .andExpect(jsonPath("$.wrappingPaperName", is("친환경 포장지")))
                .andExpect(jsonPath("$.wrappingPaperPrice", is(2000)));

        verify(wrappingPaperService, times(1)).createWrappingPaper(any(WrappingPaperRequestDto.class));
    }

    // 2. R (Read - List) - 전체 목록 조회 테스트
    @Test
    @DisplayName("GET /admin/wrappapers - 전체 목록 조회 성공 (200 OK)")
    void getAllWrappingPapers_success() throws Exception {
        WrappingPaperResponseDto dto1 = createResponseDto();
        WrappingPaperResponseDto dto2 = new WrappingPaperResponseDto(
            2L, 
            "기본 포장", 
            1000, 
            "/images/wrap/basic.png"
        );
        List<WrappingPaperResponseDto> list = List.of(dto1, dto2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<WrappingPaperResponseDto> page = new PageImpl<>(list, pageable, 2);

        when(wrappingPaperService.getAllWrappingPapers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()", is(2)))
            .andExpect(jsonPath("$.content[0].wrappingPaperName", is("고급 포장지")));

        verify(wrappingPaperService, times(1)).getAllWrappingPapers(any(Pageable.class));
    }

    // 3. R (Read - Single) - 단건 조회 테스트
    @Test
    @DisplayName("GET /admin/wrappapers/{id} - 단건 조회 성공 (200 OK)")
    void getWrappingPaper_success() throws Exception {
        WrappingPaperResponseDto responseDto = createResponseDto();
        when(wrappingPaperService.getWrappingPaper(anyLong())).thenReturn(responseDto);

        mockMvc.perform(get(BASE_URL + "/{wrappingPaperId}", TEST_WRAPPER_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wrappingPaperId", is(TEST_WRAPPER_ID.intValue())))
            .andExpect(jsonPath("$.wrappingPaperPrice", is(5000)));

        verify(wrappingPaperService, times(1)).getWrappingPaper(TEST_WRAPPER_ID);
    }

    // 4. U (Update) - 포장지 수정 테스트
    @Test
    @DisplayName("PUT /admin/wrappapers/{id} - 포장지 수정 성공 (200 OK)")
    void updateWrappingPaper_success() throws Exception {
        WrappingPaperUpdateRequestDto requestDto = new WrappingPaperUpdateRequestDto(
            "수정된 포장지 이름", 
            6000, 
            "/images/wrap/updated.png"
        );
        WrappingPaperResponseDto updatedDto = new WrappingPaperResponseDto(
            TEST_WRAPPER_ID, 
            "수정된 포장지 이름", 
            6000, 
            "/images/wrap/updated.png"
        );

        when(wrappingPaperService.updateWrappingPaper(anyLong(), any(WrappingPaperUpdateRequestDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put(BASE_URL + "/{wrappingPaperId}", TEST_WRAPPER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                        .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wrappingPaperName", is("수정된 포장지 이름")))
            .andExpect(jsonPath("$.wrappingPaperPrice", is(6000)));

        verify(wrappingPaperService, times(1)).updateWrappingPaper(anyLong(), any(WrappingPaperUpdateRequestDto.class));
    }

    // 5. D (Delete) - 포장지 삭제 테스트
    @Test
    @DisplayName("DELETE /admin/wrappapers/{id} - 포장지 삭제 성공 (204 No Content)")
    void deleteWrappingPaper_success() throws Exception {
        doNothing().when(wrappingPaperService).deleteWrappingPaper(anyLong());

        mockMvc.perform(delete(BASE_URL + "/{wrappingPaperId}", TEST_WRAPPER_ID)
                        .with(csrf()))
            .andExpect(status().isNoContent()); // 204 No Content 확인

        verify(wrappingPaperService, times(1)).deleteWrappingPaper(TEST_WRAPPER_ID);
    }
}