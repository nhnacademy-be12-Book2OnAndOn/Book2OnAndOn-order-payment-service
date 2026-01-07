package com.nhnacademy.book2onandon_order_payment_service.order.controller;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.wrapping.WrappingPaperUpdateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.WrappingPaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/wrappapers")
public class WrappingPaperAdminController {

    private final WrappingPaperService wrappingPaperService;

    // C: 포장지 등록 (POST /admin/wrappapers)
    @PostMapping
    public ResponseEntity<WrappingPaperResponseDto> createWrappingPaper(
        @Valid @RequestBody WrappingPaperRequestDto requestDto
    ) {
        WrappingPaperResponseDto response = wrappingPaperService.createWrappingPaper(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // R: 포장지 전체 목록 조회 (GET /admin/wrappapers)
    @GetMapping
    public ResponseEntity<Page<WrappingPaperResponseDto>> getAllWrappingPapers(
        @PageableDefault(size = 10, sort = "wrappingPaperId") Pageable pageable
    ) {
        Page<WrappingPaperResponseDto> page = wrappingPaperService.getAllWrappingPapers(pageable);
        return ResponseEntity.ok(page);
    }

    // R: 포장지 단건 조회 (GET /admin/wrappapers/{id})
    @GetMapping("/{wrappingPaperId}")
    public ResponseEntity<WrappingPaperResponseDto> getWrappingPaper(
        @PathVariable Long wrappingPaperId
    ) {
        WrappingPaperResponseDto response = wrappingPaperService.getWrappingPaper(wrappingPaperId);
        return ResponseEntity.ok(response);
    }

    // U: 포장지 수정 (PUT /admin/wrappapers/{id})
    @PutMapping("/{wrappingPaperId}")
    public ResponseEntity<WrappingPaperResponseDto> updateWrappingPaper(
        @PathVariable Long wrappingPaperId,
        @Valid @RequestBody WrappingPaperUpdateRequestDto requestDto
    ) {
        WrappingPaperResponseDto response = wrappingPaperService.updateWrappingPaper(wrappingPaperId, requestDto);
        return ResponseEntity.ok(response);
    }

    // D: 포장지 삭제 (DELETE /admin/wrappapers/{id})
    @DeleteMapping("/{wrappingPaperId}")
    public ResponseEntity<Void> deleteWrappingPaper(
        @PathVariable Long wrappingPaperId
    ) {
        wrappingPaperService.deleteWrappingPaper(wrappingPaperId);
        return ResponseEntity.noContent().build(); // 204 No Content 반환
    }
}