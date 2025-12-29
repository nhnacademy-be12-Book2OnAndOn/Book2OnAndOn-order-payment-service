package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundSearchCondition;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response.RefundResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundStatusUpdateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/refunds")
@PreAuthorize("hasRole('ORDER_ADMIN')") // 관리자 권한 필요
public class RefundAdminController {

    private final RefundService refundService;

    // 관리자 반품 목록 조회
    // GET /admin/refunds?status=0&startDate=2025-01-01&endDate=2025-01-31&userId=1&orderNumber=2025123456789&includeGuest=true&page=0&size=20
    // [변경사항] 위 파라미터를 Dto로 감쌈!
    @GetMapping
    public ResponseEntity<Page<RefundResponseDto>> getRefundList(
            @ModelAttribute RefundSearchCondition condition,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                refundService.getRefundListForAdmin(condition, pageable)
        );
    }

    // 관리자 반품 상세 조회
    // GET /admin/refund/{refundId}
    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponseDto> findRefundDetails(@PathVariable Long refundId) {
        RefundResponseDto response = refundService.getRefundDetailsForAdmin(refundId);
        return ResponseEntity.ok(response);
    }

    // 관리자 반품 상태 변경
    // PATCH /admin/refund/{refundId}
    @PatchMapping("/{refundId}")
    public ResponseEntity<RefundResponseDto> updateRefundStatus(
            @PathVariable Long refundId,
            @Valid @RequestBody RefundStatusUpdateRequestDto request
    ) {
        return ResponseEntity.ok(refundService.updateRefundStatus(refundId, request));
    }
}