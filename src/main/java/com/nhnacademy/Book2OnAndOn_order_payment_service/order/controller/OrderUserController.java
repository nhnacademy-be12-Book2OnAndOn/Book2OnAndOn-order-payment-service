package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders") // Base Path
public class OrderUserController {

    private final OrderService orderService;

    // POST /api/orders (회원 주문 생성)
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderCreateRequestDto request) {
        OrderResponseDto response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    // GET /api/orders (회원 내 주문 목록 조회)
    @GetMapping
    public ResponseEntity<Page<OrderSimpleDto>> findOrderList(Pageable pageable) {
        //  실제로는 Spring Security Context에서 사용자 ID를 가져와야 함
        Long currentUserId = 1L; 
        Page<OrderSimpleDto> response = orderService.findOrderList(currentUserId, pageable);
        return ResponseEntity.ok(response);
    }

    // GET /api/orders/{orderId} (회원 내 주문 상세 조회)
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> findOrderDetails(@PathVariable Long orderId) {
        //  사용자 ID 검증 로직은 Service에서 처리
        OrderResponseDto response = orderService.findOrderDetails(orderId, 1L); 
        return ResponseEntity.ok(response);
    }

    // PATCH /api/orders/{orderId} (회원 주문 취소)
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long orderId, 
                                                        @Valid @RequestBody OrderCancelRequestDto request) {
        OrderResponseDto response = orderService.cancelOrder(orderId, 1L, request); //  사용자 ID 검증
        return ResponseEntity.ok(response);
    }
}