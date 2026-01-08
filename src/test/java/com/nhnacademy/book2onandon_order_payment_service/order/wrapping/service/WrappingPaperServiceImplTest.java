package com.nhnacademy.book2onandon_order_payment_service.order.wrapping.service;

import com.nhnacademy.book2onandon_order_payment_service.exception.NotFoundException;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperSimpleResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.wrapping.WrappingPaperRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.impl.WrappingPaperServiceImpl;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WrappingPaperServiceImplTest {

    @Mock
    private WrappingPaperRepository wrappingPaperRepository;

    @InjectMocks
    private WrappingPaperServiceImpl wrappingPaperService;

    private WrappingPaper mockEntity;
    private static final Long TEST_ID = 1L;
    private static final String TEST_NAME = "고급 포장지";
    private static final int TEST_PRICE = 5000;
    private static final String TEST_PATH = "/images/premium.png";

    @BeforeEach
    void setUp() {
        mockEntity = WrappingPaper.create(TEST_NAME, TEST_PRICE, TEST_PATH);
    }

    @Test
    @DisplayName("포장지 등록 성공: createWrappingPaper")
    void createWrappingPaper_success() {
        WrappingPaperRequestDto requestDto = new WrappingPaperRequestDto(
            "친환경 포장지", 2000, "/images/eco.png"
        );
        when(wrappingPaperRepository.save(any(WrappingPaper.class))).thenReturn(mockEntity);

        WrappingPaperResponseDto result = wrappingPaperService.createWrappingPaper(requestDto);

        assertThat(result.getWrappingPaperName()).isEqualTo(TEST_NAME);
        assertThat(result.getWrappingPaperPrice()).isEqualTo(TEST_PRICE);
        verify(wrappingPaperRepository, times(1)).save(any(WrappingPaper.class));
    }

    @Test
    @DisplayName("포장지 단건 조회 성공: getWrappingPaper")
    void getWrappingPaper_success() {
        WrappingPaper localMock = mock(WrappingPaper.class);
        WrappingPaperResponseDto mockResponseDto = new WrappingPaperResponseDto(TEST_ID, TEST_NAME, TEST_PRICE, TEST_PATH);

        lenient().when(localMock.getWrappingPaperId()).thenReturn(TEST_ID);
        lenient().when(localMock.toResponseDto()).thenReturn(mockResponseDto);

        when(wrappingPaperRepository.findById(TEST_ID)).thenReturn(Optional.of(localMock));

        WrappingPaperResponseDto result = wrappingPaperService.getWrappingPaper(TEST_ID);

        assertThat(result.getWrappingPaperId()).isEqualTo(TEST_ID);
        assertThat(result.getWrappingPaperName()).isEqualTo(TEST_NAME);

        verify(wrappingPaperRepository, times(1)).findById(TEST_ID);
    }

    @Test
    @DisplayName("존재하지 않는 포장지 조회 시 예외 발생: getWrappingPaper")
    void getWrappingPaper_notFound() {
        when(wrappingPaperRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            wrappingPaperService.getWrappingPaper(999L);
        });
        verify(wrappingPaperRepository, times(1)).findById(anyLong());
    }

    @Test
    @DisplayName("사용자 포장지 목록 조회 성공 (경량 DTO): getWrappingPaperList")
    void getWrappingPaperList_success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("wrappingPaperId"));
        List<WrappingPaper> list = List.of(mockEntity, mockEntity);
        Page<WrappingPaper> entityPage = new PageImpl<>(list, pageable, 2);

        when(wrappingPaperRepository.findAll(any(Pageable.class))).thenReturn(entityPage);


        Page<WrappingPaperSimpleResponseDto> resultPage = wrappingPaperService.getWrappingPaperList(pageable);

        assertThat(resultPage).hasSize(2);
        assertThat(resultPage.getContent().get(0).getWrappingPaperName()).isEqualTo(TEST_NAME);

        verify(wrappingPaperRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 포장지 전체 목록 조회 성공: getAllWrappingPapers")
    void getAllWrappingPapers_success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<WrappingPaper> list = List.of(mockEntity);
        Page<WrappingPaper> entityPage = new PageImpl<>(list, pageable, 1);

        when(wrappingPaperRepository.findAll(any(Pageable.class))).thenReturn(entityPage);

        Page<WrappingPaperResponseDto> resultPage = wrappingPaperService.getAllWrappingPapers(pageable);

        assertThat(resultPage).hasSize(1);
        assertThat(resultPage.getContent().get(0).getWrappingPaperPath()).isEqualTo(TEST_PATH);
        verify(wrappingPaperRepository, times(1)).findAll(any(Pageable.class));
    }


    @Test
    @DisplayName("포장지 수정 성공: updateWrappingPaper")
    void updateWrappingPaper_success() {
        String updated_name = "새로운 포장지 이름";
        int updated_price = 9999;

        WrappingPaperUpdateRequestDto requestDto = new WrappingPaperUpdateRequestDto(
            updated_name, updated_price, null
        );
        when(wrappingPaperRepository.findById(TEST_ID)).thenReturn(Optional.of(mockEntity));

        WrappingPaperResponseDto result = wrappingPaperService.updateWrappingPaper(TEST_ID, requestDto);

        assertThat(result.getWrappingPaperName()).isEqualTo(updated_name);
        assertThat(result.getWrappingPaperPrice()).isEqualTo(updated_price);

        assertThat(mockEntity.getWrappingPaperName()).isEqualTo(updated_name);

        verify(wrappingPaperRepository, times(1)).findById(TEST_ID);
    }

    @Test
    @DisplayName("수정 시 존재하지 않는 포장지 예외 발생: updateWrappingPaper")
    void updateWrappingPaper_notFound() {
        WrappingPaperUpdateRequestDto requestDto = new WrappingPaperUpdateRequestDto(
            "이름", 1000, "경로"
        );
        when(wrappingPaperRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            wrappingPaperService.updateWrappingPaper(999L, requestDto);
        });
        verify(wrappingPaperRepository, times(1)).findById(anyLong());
    }


    @Test
    @DisplayName("포장지 삭제 성공: deleteWrappingPaper")
    void deleteWrappingPaper_success() {
        doNothing().when(wrappingPaperRepository).deleteById(TEST_ID);

        wrappingPaperService.deleteWrappingPaper(TEST_ID);

        verify(wrappingPaperRepository, times(1)).deleteById(TEST_ID);
    }

    @Test
    @DisplayName("Entity 조회 성공: getWrappingPaperEntity")
    void getWrappingPaperEntity_success() {
        WrappingPaper mock = mock(WrappingPaper.class);

        when(mock.getWrappingPaperId()).thenReturn(TEST_ID);

        when(wrappingPaperRepository.findById(TEST_ID)).thenReturn(Optional.of(mock));

        WrappingPaper result = wrappingPaperService.getWrappingPaperEntity(TEST_ID);

        assertThat(result).isNotNull();
        assertThat(result.getWrappingPaperId()).isEqualTo(TEST_ID);
        verify(wrappingPaperRepository, times(1)).findById(TEST_ID);
    }
}