package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundGuestRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundAvailableItemResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guest/orders/{orderId}/refunds")
public class RefundGuestController {

    private final RefundService refundService;

    // 비회원 반품 신청
    // POST /guest/orders/{orderId}/refunds
    @PostMapping
    public ResponseEntity<RefundResponseDto> createRefundForGuest(
            @PathVariable Long orderId,
            @Valid @RequestBody RefundGuestRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.createRefundForGuest(orderId, dto));
    }

    // 비회원 반품 신청 취소
    // POST /guest/orders/{orderId}/refunds/{refundId}/cancel
    @PostMapping("/{refundId}/cancel")
    public ResponseEntity<RefundResponseDto> cancelRefundForGuest(
            @PathVariable Long orderId,
            @PathVariable Long refundId,
            @RequestParam String guestPassword
    ) {
        return ResponseEntity.ok(
                refundService.cancelRefundForGuest(orderId, refundId, guestPassword)
        );
    }

    // 비회원 반품 상세 조회
    // GET /guest/orders/{orderId}/refunds/{refundId}
    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponseDto> getRefundDetailsForGuest(
            @PathVariable Long orderId,
            @PathVariable Long refundId
    ) {
        return ResponseEntity.ok(refundService.getRefundDetailsForGuest(orderId, refundId));
    }

    // 비회원 반품 신청 폼: 반품 가능 품목 목록
    // GET /guest/orders/{orderId}/refunds/form
    @GetMapping("/form")
    public ResponseEntity<List<RefundAvailableItemResponseDto>> getRefundFormForGuest(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(refundService.getRefundableItemsForGuest(orderId));
    }

}
