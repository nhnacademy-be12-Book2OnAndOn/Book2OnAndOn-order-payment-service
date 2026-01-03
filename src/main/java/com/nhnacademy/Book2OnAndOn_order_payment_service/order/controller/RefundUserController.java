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
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
public class RefundUserController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private final RefundService refundService;

    // 회원 반품 신청
    @PostMapping("/orders/{orderId}/refunds")
    public ResponseEntity<RefundResponseDto> createRefund(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody RefundRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(refundService.createRefundForMember(orderId, userId, request));
    }

    // 회원 반품 신청 취소
    @PostMapping("/orders/{orderId}/refunds/{refundId}/cancel")
    public ResponseEntity<RefundResponseDto> cancelRefund(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long orderId,
            @PathVariable Long refundId
    ) {
        return ResponseEntity.ok(refundService.cancelRefundForMember(orderId, refundId, userId));
    }

    // 회원 반품 상세 조회
    @GetMapping("/orders/{orderId}/refunds/{refundId}")
    public ResponseEntity<RefundResponseDto> getRefundDetails(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long orderId,
            @PathVariable Long refundId
    ) {
        return ResponseEntity.ok(refundService.getRefundDetailsForMember(orderId, refundId, userId));
    }

    // 회원 전체 반품 목록 조회
    @GetMapping("/orders/refunds/my-list")
    public ResponseEntity<Page<RefundResponseDto>> getMyRefunds(
            @RequestHeader(USER_ID_HEADER) Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(refundService.getRefundsForMember(userId, pageable));
    }

    // 회원 반품 신청 폼
    @GetMapping("/orders/{orderId}/refunds/form")
    public ResponseEntity<List<RefundAvailableItemResponseDto>> getRefundForm(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(refundService.getRefundableItemsForMember(orderId, userId));
    }
}
