package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrapPaperRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrapPaperResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrapPaperSimpleResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.WrappingPaperService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wrappapers")
public class WrappingPaperController {

    private final WrappingPaperService wrappingPaperService;

    // ======================================================================
    // 공용 API: 목록 조회 (모든 사용자 접근 가능)
    // ======================================================================

    /**
     * [GET] /wrappapers (공용 포장지 목록 조회)
     */
    @GetMapping
    public ResponseEntity<Page<WrapPaperSimpleResponseDto>> getWrappingPaperList(Pageable pageable) {
        Page<WrapPaperSimpleResponseDto> response = wrappingPaperService.getWrappingPaperList(pageable);
        return ResponseEntity.ok(response);
    }

    // ======================================================================
    // 관리자 API: CRUD
    // ======================================================================

    /**
     * [POST] /wrappapers (관리자 포장지 생성)
     */
    @PostMapping
    @PreAuthorize("hasRole('ORDER_ADMIN')") // 관리자 권한 필요
    public ResponseEntity<WrapPaperResponseDto> createWrappingPaper(@Valid @RequestBody WrapPaperRequestDto request) {
        WrapPaperResponseDto response = wrappingPaperService.createWrappingPaper(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * [PUT] /wrappapers/{wrappingPaperId} (관리자 포장지 정보 수정)
     */
    @PutMapping("/{wrappingPaperId}")
    @PreAuthorize("hasRole('ORDER_ADMIN')") //  관리자 권한 필요
    public ResponseEntity<WrapPaperResponseDto> updateWrappingPaper(
        @PathVariable Long wrappingPaperId, 
        @Valid @RequestBody WrapPaperRequestDto request) {
        
        WrapPaperResponseDto response = wrappingPaperService.updateWrappingPaper(wrappingPaperId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * [DELETE] /wrappapers/{wrappingPaperId} (관리자 포장지 삭제)
     */
    @DeleteMapping("/{wrappingPaperId}")
    @PreAuthorize("hasRole('ORDER_ADMIN')") // 관리자 권한 필요
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content
    public void deleteWrappingPaper(@PathVariable Long wrappingPaperId) {
        wrappingPaperService.deleteWrappingPaper(wrappingPaperId);
    }
}