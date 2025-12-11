package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders/{orderId}/refund")
public class RefundUserController {

    private final RefundService refundService;

    // Security Authentication에서 userId 추출
    private Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("인증된 사용자 정보가 없습니다.");
        }
        return Long.valueOf(authentication.getName());
    }

    // 회원 반품 신청
    // POST /orders/{orderId}/refund
    @PostMapping
    public ResponseEntity<RefundResponseDto> createRefund(
            @PathVariable Long orderId,
            @Valid @RequestBody RefundRequestDto request,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        RefundResponseDto response = refundService.createRefundForMember(orderId, userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 회원 반품 상세 조회
    // GET /orders/{orderId}/refund/{refundId}
    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponseDto> getRefundDetails(
            @PathVariable Long orderId,   // 현재 구현에서는 사용 X, 필요하면 검증 로직 추가
            @PathVariable Long refundId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        RefundResponseDto response = refundService.getRefundDetailsForMember(userId, refundId);
        return ResponseEntity.ok(response);
    }

    // 회원 전체 반품 목록 조회
    // GET /orders/{orderId}/returns/list?page=0&size=20
    @GetMapping("/list")
    public ResponseEntity<Page<RefundResponseDto>> getMyRefunds(
            @PathVariable Long orderId,   // 현재 구현에서는 사용 X
            Authentication authentication,
            Pageable pageable
    ) {
        Long userId = getUserId(authentication);
        Page<RefundResponseDto> page = refundService.getRefundsForMember(userId, pageable);
        return ResponseEntity.ok(page);
    }
}