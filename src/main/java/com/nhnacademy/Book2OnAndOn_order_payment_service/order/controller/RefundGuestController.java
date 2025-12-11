package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guest/orders/{orderId}/refund")
public class RefundGuestController {

    private final RefundService refundService;

    // 비회원 반품 신청
    // POST /guest/orders/{orderId}/refund
    @PostMapping
    public ResponseEntity<RefundResponseDto> createRefundForGuest(
            @PathVariable Long orderId,
            @Valid @RequestBody RefundRequestDto request
    ) {
        RefundResponseDto response = refundService.createRefundForGuest(orderId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 비회원 반품 상세 조회
    // GET /guest/orders/{orderId}/refund/{refundId}
    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponseDto> getRefundDetailsForGuest(
            @PathVariable Long orderId,
            @PathVariable Long refundId
    ) {
        RefundResponseDto response = refundService.getRefundDetailsForGuest(refundId);
        return ResponseEntity.ok(response);
    }

}
