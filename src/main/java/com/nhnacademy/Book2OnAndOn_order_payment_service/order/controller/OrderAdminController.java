package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/orders") // Base Path
@PreAuthorize("hasRole('ORDER_ADMIN')") 
public class OrderAdminController {

    private final OrderService orderService;

    // GET /admin/orders (관리자 전체 주문 목록 조회)
    @GetMapping
    public ResponseEntity<Page<OrderSimpleDto>> findAllOrderList(Pageable pageable) {
        // Service에서 모든 주문 목록을 조회
        Page<OrderSimpleDto> response = orderService.findAllOrderList(pageable);
        return ResponseEntity.ok(response);
    }

    // GET /admin/orders/{orderId} (관리자 특정 주문 상세 조회)
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> findAdminOrderDetails(@PathVariable Long orderId) {
        // 관리자용 상세 조회는 사용자 ID 검증 없이 주문 ID만으로 조회
        OrderResponseDto response = orderService.findOrderDetails(orderId, null); 
        return ResponseEntity.ok(response);
    }

    // PATCH /admin/orders/{orderId} (관리자 주문 상태 변경)
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> updateOrderStatusByAdmin(@PathVariable Long orderId,
                                                                     @Valid @RequestBody OrderStatusUpdateDto request) {
        OrderResponseDto response = orderService.updateOrderStatusByAdmin(orderId, request);
        return ResponseEntity.ok(response);
    }
}