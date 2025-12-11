package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrappingPaperRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrappingPaperResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrappingPaperSimpleResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrappingPaperUpdateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WrappingPaperService {

    // C: 포장지 생성
    WrappingPaperResponseDto createWrappingPaper(WrappingPaperRequestDto requestDto);

    // R: 포장지 단건 조회
    WrappingPaperResponseDto getWrappingPaper(Long wrappingPaperId);

    // R: 포장지 전체 목록 조회 (사용자용, 경량 DTO 반환)
    Page<WrappingPaperSimpleResponseDto> getWrappingPaperList(Pageable pageable);

    // R: 포장지 전체 목록 조회 (관리자용)
    Page<WrappingPaperResponseDto> getAllWrappingPapers(Pageable pageable);

    // U: 포장지 수정
    WrappingPaperResponseDto updateWrappingPaper(Long wrappingPaperId, WrappingPaperUpdateRequestDto requestDto);

    // D: 포장지 삭제
    void deleteWrappingPaper(Long wrappingPaperId);

    // 포장비 계산을 위해 OrderService에서 호출
    WrappingPaper getWrappingPaperEntity(Long wrappingPaperId);

}