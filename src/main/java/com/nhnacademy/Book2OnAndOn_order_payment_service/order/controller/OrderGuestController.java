package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestOrderCreateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guest/orders")
public class OrderGuestController {

    private final OrderService orderService;

    // POST /api/guest/orders (비회원 주문 생성)
    @PostMapping
    public ResponseEntity<OrderResponseDto> createGuestOrder(@Valid @RequestBody GuestOrderCreateDto request) {
        OrderResponseDto response = orderService.createGuestOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    // GET /api/guest/orders/{orderId} (비회원 주문 상세 조회)
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> findGuestOrderDetails(@PathVariable Long orderId,
                                                               @RequestParam("password") String password) {
        OrderResponseDto response = orderService.findGuestOrderDetails(orderId, password);
        return ResponseEntity.ok(response);
    }
    
    // PATCH /api/guest/orders/{orderId} (비회원 주문 취소)
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> cancelGuestOrder(@PathVariable Long orderId,
                                                           @RequestParam("password") String password) {
        OrderResponseDto response = orderService.cancelGuestOrder(orderId, password);
        return ResponseEntity.ok(response);
    }
}