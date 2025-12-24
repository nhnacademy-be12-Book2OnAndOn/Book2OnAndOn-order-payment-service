package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
import java.util.List;
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
@RequestMapping("/orders/{orderId}/refunds")
public class RefundUserController {

    private final RefundService refundService;

    // Security Authentication에서 userId 추출
    private Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("인증된 사용자 정보가 없습니다.");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("인증 정보에서 userId를 추출할 수 없습니다.");
        }
    }

    // 회원 반품 신청
    // POST /orders/{orderId}/refunds
    @PostMapping
    public ResponseEntity<RefundResponseDto> createRefund(
            @PathVariable Long orderId,
            @Valid @RequestBody RefundRequestDto request,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.createRefundForMember(orderId, userId, request));
    }

    // 회원 반품 신청 취소
    // POST /orders/{orderId}/refunds/{refundId}/cancel
    @PostMapping("/{refundId}/cancel")
    public ResponseEntity<RefundResponseDto> cancelRefund(
            @PathVariable Long orderId,
            @PathVariable Long refundId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(refundService.cancelRefundForMember(orderId, refundId, userId));
    }

    // 회원 반품 상세 조회
    // GET /orders/{orderId}/refund/{refundId}
    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponseDto> getRefundDetails(
            @PathVariable Long orderId,
            @PathVariable Long refundId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(refundService.getRefundDetailsForMember(orderId, refundId, userId
        ));
    }


    // 회원 전체 반품 목록 조회
    // GET /orders/{orderId}/returns/list?page=0&size=20
    @GetMapping("/list")
    public ResponseEntity<Page<RefundResponseDto>> getMyRefunds(
            Authentication authentication,
            Pageable pageable
    ) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(refundService.getRefundsForMember(userId, pageable));
    }

    // 회원 반품 신청 폼
    @GetMapping("/form")
    public ResponseEntity<List<RefundAvailableItemResponseDto>> getRefundForm(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(refundService.getRefundableItemsForMember(orderId, userId));
    }
}