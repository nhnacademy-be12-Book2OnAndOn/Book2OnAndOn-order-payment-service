package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1.ReturnStatusUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/returns")
@PreAuthorize("hasRole('ORDER_ADMIN')") // 관리자 권한 필요
public class ReturnAdminController {

    private final ReturnService returnService;

    // GET /api/admin/returns/{returnId} (관리자 반품 상세 조회)
    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnResponseDto> findAdminReturnDetails(@PathVariable Long returnId) {
        // 관리자는 userId=null로 전달하여 권한 검증을 건너뜁니다.
        ReturnResponseDto response = returnService.getReturnDetails(returnId, null);
        return ResponseEntity.ok(response);
    }

    // PATCH /api/admin/returns/{returnId} (관리자 반품 상태 변경)
    @PatchMapping("/{returnId}")
    public ResponseEntity<ReturnResponseDto> updateReturnStatus(
            @PathVariable Long returnId,
            @Valid @RequestBody ReturnStatusUpdateDto request
    ) {
        ReturnResponseDto response = returnService.updateReturnStatusByAdmin(returnId, request);
        return ResponseEntity.ok(response);
    }
}