package com.nhnacademy.book2onandon_order_payment_service.order.service.impl;

import com.nhnacademy.book2onandon_order_payment_service.exception.NotFoundException;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperSimpleResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.wrapping.WrappingPaperRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.WrappingPaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WrappingPaperServiceImpl implements WrappingPaperService {
    
    private final WrappingPaperRepository wrappingPaperRepository;
    private static final String WRAPPING_PAPER_NOT_FOUND = "WrappingPaper not found: ";

    // 포장지 생성
    @Override
    @Transactional
    public WrappingPaperResponseDto createWrappingPaper(WrappingPaperRequestDto requestDto) {
        WrappingPaper entity = WrappingPaper.create(requestDto.getWrappingPaperName(),
                                                    requestDto.getWrappingPaperPrice(),
                                                    requestDto.getWrappingPaperPath()
        );
        WrappingPaper savedEntity = wrappingPaperRepository.save(entity);
        return savedEntity.toResponseDto();
    }

    // 포장지 단건 조회
    @Override
    @Transactional(readOnly = true)
    public WrappingPaperResponseDto getWrappingPaper(Long wrappingPaperId) {
        WrappingPaper entity = wrappingPaperRepository.findById(wrappingPaperId)
            .orElseThrow(() -> new NotFoundException(WRAPPING_PAPER_NOT_FOUND + wrappingPaperId));
        return entity.toResponseDto();
    }

    // 포장지 전체 목록 조회 (사용자용, 경량 DTO 반환)
    @Override
    @Transactional(readOnly = true)
    public Page<WrappingPaperSimpleResponseDto> getWrappingPaperList(Pageable pageable) {
        Page<WrappingPaper> entityPage = wrappingPaperRepository.findAll(pageable);

        return entityPage.map(entity -> new WrappingPaperSimpleResponseDto(
                entity.getWrappingPaperId(),
                entity.getWrappingPaperName(),
                entity.getWrappingPaperPrice()
        ));
    }

    // 포장지 전체 목록 조회 (관리자용)
    @Override
    @Transactional(readOnly = true)
    public Page<WrappingPaperResponseDto> getAllWrappingPapers(Pageable pageable) {
        return wrappingPaperRepository.findAll(pageable)
            .map(WrappingPaper::toResponseDto);
    }

    // 포장지 수정
    @Override
    @Transactional
    public WrappingPaperResponseDto updateWrappingPaper(Long wrappingPaperId, WrappingPaperUpdateRequestDto requestDto) {
        WrappingPaper entity = wrappingPaperRepository.findById(wrappingPaperId)
            .orElseThrow(() -> new NotFoundException(WRAPPING_PAPER_NOT_FOUND + wrappingPaperId));
        
        entity.update(requestDto.getWrappingPaperName(),
                      requestDto.getWrappingPaperPrice(),
                      requestDto.getWrappingPaperPath());
        
        return entity.toResponseDto();
    }

    // 포장지 삭제
    @Override
    @Transactional
    public void deleteWrappingPaper(Long wrappingPaperId) {
        wrappingPaperRepository.deleteById(wrappingPaperId);
    }

    // 이 메서드는 OrderService에서만 사용해야 하므로 DTO가 아닌 Entity를 반환
    @Override
    @Transactional(readOnly = true)
    public WrappingPaper getWrappingPaperEntity(Long wrappingPaperId) {
        return wrappingPaperRepository.findById(wrappingPaperId)
                .orElseThrow(() -> new NotFoundException(WRAPPING_PAPER_NOT_FOUND + wrappingPaperId));
    }
}