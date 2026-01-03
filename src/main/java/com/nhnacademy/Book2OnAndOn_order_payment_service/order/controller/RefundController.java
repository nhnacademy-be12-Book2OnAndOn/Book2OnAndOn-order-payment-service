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
public class RefundController {

    private final RefundService refundService;

    // 반품 신청
    // POST /orders/{orderId}/refunds
    @PostMapping("/orders/{orderId}/refunds")
    public ResponseEntity<RefundResponseDto> createRefund(
            @PathVariable Long orderId,
            @Valid @RequestBody RefundRequestDto request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Guest-Order-Token", required = false) String guestToken

    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.createRefund(orderId, userId, request, guestToken));
    }

    // 반품 신청 취소
    // POST /orders/{orderId}/refunds/{refundId}/cancel
    @PostMapping("/orders/{orderId}/refunds/{refundId}/cancel")
    public ResponseEntity<RefundResponseDto> cancelRefund(
            @PathVariable Long orderId,
            @PathVariable Long refundId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Guest-Order-Token", required = false) String guestToken
    ) {

        return ResponseEntity.ok(refundService.cancelRefund(orderId, refundId, userId, guestToken));
    }

    // 반품 상세 조회
    // GET /orders/{orderId}/refund/{refundId}
    @GetMapping("/orders/{orderId}/refund/{refundId}")
    public ResponseEntity<RefundResponseDto> getRefundDetails(
            @PathVariable Long orderId,
            @PathVariable Long refundId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Guest-Order-Token", required = false) String guestToken
    ) {
        return ResponseEntity.ok(refundService.getRefundDetails(orderId, refundId, userId, guestToken));
    }

    // 반품 신청 폼
    @GetMapping("/orders/{orderId}/refunds/form")
    public ResponseEntity<List<RefundAvailableItemResponseDto>> getRefundForm(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Guest-Order-Token", required = false) String guestToken

    ) {
        return ResponseEntity.ok(refundService.getRefundableItems(orderId, userId, guestToken));
    }

    // 회원 전체 반품 목록 조회
    // GET /orders/{orderId}/returns/list?page=0&size=20
    @GetMapping("/orders/refunds/my-list")
    public ResponseEntity<Page<RefundResponseDto>> getMyRefunds(
            @RequestHeader("X-User-Id") Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(refundService.getRefundsForMember(userId, pageable));
    }

}