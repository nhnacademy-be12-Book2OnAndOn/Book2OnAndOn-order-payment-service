package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.wrapping.WrappingPaperSimpleResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.WrappingPaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 일반 사용자(비회원 포함)가 주문서에서 포장지 옵션 조회
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/wrappapers")
public class WrappingPaperController {

    private final WrappingPaperService wrappingPaperService;

    @GetMapping
    public ResponseEntity<Page<WrappingPaperSimpleResponseDto>> getWrappingPaperList(
            @PageableDefault(size = 20, sort = "wrappingPaperId", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        log.info("GET /wrappapers 호출: 사용자 포장지 목록 조회");

        Page<WrappingPaperSimpleResponseDto> page = wrappingPaperService.getWrappingPaperList(pageable);

        return ResponseEntity.ok(page);
    }
}