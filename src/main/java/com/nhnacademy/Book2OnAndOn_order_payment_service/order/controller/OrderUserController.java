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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders") // Base Path
public class OrderUserController {

    private final OrderService orderService;

    // 유틸리티 메서드: String(username)을 Long(userId)으로 변환
    private Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // 이 API는 인증이 필수이므로, 인증 실패 시 403이 먼저 발생해야 함.
            // 여기서는 안전하게 Long으로 변환 가능한 ID를 가정
            throw new AccessDeniedException("인증된 사용자 정보가 없습니다.");
        }
        return Long.valueOf(authentication.getName());
    }
    // POST /api/orders (회원 주문 생성)
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderCreateRequestDto request, @RequestHeader("X-USER-ID")Long userId) {
        // 요청 본문의 userId를 무시하고 인증된 사용자의 ID로 강제함
        OrderResponseDto response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    // GET /api/orders (회원 내 주문 목록 조회)
    @GetMapping
    public ResponseEntity<Page<OrderSimpleDto>> findOrderList(Authentication authentication, Pageable pageable) {
        //  실제로는 Spring Security Context에서 사용자 ID를 가져와야 함
        Long currentUserId = Long.valueOf(authentication.getName());
        Page<OrderSimpleDto> response = orderService.findOrderList(currentUserId, pageable);
        return ResponseEntity.ok(response);
    }

    // GET /api/orders/{orderId} (회원 내 주문 상세 조회)
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> findOrderDetails(@PathVariable Long orderId, Authentication authentication) {
        //  사용자 ID 검증 로직은 Service에서 처리
        Long currentUserId = Long.valueOf(authentication.getName());
        OrderResponseDto response = orderService.findOrderDetails(orderId, currentUserId);
        return ResponseEntity.ok(response);
    }

    // PATCH /api/orders/{orderId} (회원 주문 취소)
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long orderId, 
                                                        @Valid @RequestBody OrderCancelRequestDto request,Authentication authentication) {
        Long currentUserId = getUserId(authentication);
        OrderResponseDto response = orderService.cancelOrder(orderId, currentUserId, request); //  사용자 ID 검증
        return ResponseEntity.ok(response);
    }
}