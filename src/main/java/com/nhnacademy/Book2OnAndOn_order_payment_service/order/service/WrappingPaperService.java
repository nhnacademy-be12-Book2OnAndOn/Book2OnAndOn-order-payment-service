package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.WrappingPaperNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.wrapping.WrappingPaperRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포장지 정보 생성, 수정, 조회, 삭제 담당 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WrappingPaperService {

    private final WrappingPaperRepository wrappingPaperRepository;
    private final OrderItemRepository orderItemRepository;

    // ======================================================================
    // 1. 포장지 생성 및 수정
    // ======================================================================

    /**
     * 새로운 포장지 정보 생성, 저장
     */
    @Transactional
    public WrapPaperResponseDto createWrappingPaper(WrapPaperRequestDto request) {
        // 1. 엔티티 생성
        WrappingPaper wrappingPaper = WrappingPaper.builder()
                .wrappingPaperName(request.getWrappingPaperName())
                .wrappingPaperPrice(request.getWrappingPaperPrice())
                .wrappingPaperPath(request.getWrappingPaperPath())
                .build();

        // 2. 저장
        WrappingPaper saved = wrappingPaperRepository.save(wrappingPaper);
        
        return convertToResponseDto(saved);
    }

    /**
     * 기존 포장지 정보 수정
     */
    @Transactional
    public WrapPaperResponseDto updateWrappingPaper(Long wrappingPaperId, WrapPaperRequestDto request) {
        WrappingPaper existing = wrappingPaperRepository.findById(wrappingPaperId)
                .orElseThrow(() -> new WrappingPaperNotFoundException(wrappingPaperId));

        // 1. 필드 업데이트
        existing.setWrappingPaperName(request.getWrappingPaperName());
        existing.setWrappingPaperPrice(request.getWrappingPaperPrice());
        existing.setWrappingPaperPath(request.getWrappingPaperPath());

        // @Transactional이므로 save 호출 없이도 더티 체킹으로 업데이트
        return convertToResponseDto(existing);
    }

    // ======================================================================
    // 2. 조회 및 목록
    // ======================================================================

    /**
     * 모든 포장지 목록을 페이지네이션하여 조회 (공용 API)
     */
    @Transactional(readOnly = true)
    public Page<WrapPaperSimpleResponseDto> getWrappingPaperList(Pageable pageable) {
        Page<WrappingPaper> page = wrappingPaperRepository.findAll(pageable);
        
        // DTO로 변환
        return page.map(this::convertToSimpleResponseDto);
    }

    /**
     * 특정 포장지 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public WrapPaperResponseDto getWrappingPaperDetails(Long wrappingPaperId) {
        WrappingPaper wrappingPaper = wrappingPaperRepository.findById(wrappingPaperId)
                .orElseThrow(() -> new WrappingPaperNotFoundException(wrappingPaperId));
        
        return convertToResponseDto(wrappingPaper);
    }

    // ======================================================================
    // 3. 삭제
    // ======================================================================
    
    /**
     * 특정 포장지 정보 삭제
     */
    @Transactional
    public void deleteWrappingPaper(Long wrappingPaperId) {
        if (!wrappingPaperRepository.existsById(wrappingPaperId)) {
            throw new WrappingPaperNotFoundException(wrappingPaperId);
        }
        // 포장지를 참조하는 OrderItem이 있는지 검사하는 로직 추가
        long usageCount = orderItemRepository.countByWrappingPaper_WrappingPaperId(wrappingPaperId);

        if (usageCount > 0) {
            throw new IllegalStateException("해당 포장지는 " + usageCount + "개의 주문에 사용되었으므로 삭제할 수 없습니다.");
        }
        wrappingPaperRepository.deleteById(wrappingPaperId);
    }

    // ======================================================================
    // 4. DTO 변환 헬퍼
    // ======================================================================

    //생성, 저장, 수정, 상세조회시 사용
    private WrapPaperResponseDto convertToResponseDto(WrappingPaper entity) {
        return new WrapPaperResponseDto(
                entity.getWrappingPaperId(),
                entity.getWrappingPaperName(),
                entity.getWrappingPaperPrice(),
                entity.getWrappingPaperPath()
        );
    }

    // 목록 조회시 사용
    private WrapPaperSimpleResponseDto convertToSimpleResponseDto(WrappingPaper entity) {
        return new WrapPaperSimpleResponseDto(
                entity.getWrappingPaperId(),
                entity.getWrappingPaperName(),
                entity.getWrappingPaperPrice()
        );
    }
}