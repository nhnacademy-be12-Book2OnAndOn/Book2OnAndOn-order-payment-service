package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders/{orderId}/returns")
public class ReturnUserController {

    private final ReturnService returnService;

    private Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("인증된 사용자 정보가 없습니다.");
        }
        return Long.valueOf(authentication.getName());
    }
    
    // POST /orders/{orderId}/returns (회원 반품 신청)
    @PostMapping
    public ResponseEntity<ReturnResponseDto> createReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestDto request,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        ReturnResponseDto response = returnService.createReturn(orderId, userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // GET /orders/{orderId}/returns/{returnId} (회원 반품 상세 조회)
    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnResponseDto> getReturnDetails(
            @PathVariable Long orderId, // orderId는 경로에 있지만 returnId만 사용
            @PathVariable Long returnId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        ReturnResponseDto response = returnService.getReturnDetails(returnId, userId);
        return ResponseEntity.ok(response);
    }
}