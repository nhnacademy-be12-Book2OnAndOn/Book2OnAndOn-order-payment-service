package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestOrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/guest/orders")
public class OrderGuestController {

    private final OrderService orderService;
    private static final String GUEST_ID_HEADER = "X-Guest-Id";

    // Post /guest/orders/prepare (비회원 주문 준비)
    @PostMapping("/prepare")
    public ResponseEntity<OrderPrepareResponseDto> getGuestOrderPrepare(@RequestHeader(GUEST_ID_HEADER) String guestId,
                                                                        @RequestBody OrderPrepareRequestDto req){
        log.info("POST /guest/orders/prepare 호출 : 주문시 필요 데이터 반환 (Guest-ID : {})", guestId);
        OrderPrepareResponseDto resp = orderService.prepareGuestOrder(guestId, req);
        return ResponseEntity.ok(resp);
    }

    // POST /guest/orders (비회원 주문 생성)
    @PostMapping
    public ResponseEntity<OrderCreateResponseDto> createGuestOrder(@RequestHeader(GUEST_ID_HEADER) String guestId,
                                                                   @RequestBody GuestOrderCreateRequestDto req) {
        log.info("POST /guest/orders 호출 : 비회원 사전 주문 데이터 생성");
        OrderCreateResponseDto resp = orderService.createGuestPreOrder(guestId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // GET /guest/orders (비회원 주문 상세 조회)
    @GetMapping
    public ResponseEntity<OrderDetailResponseDto> findGuestOrderDetails() {
//        OrderResponseDto response = orderService.findGuestOrderDetails(orderId, password);
//        return ResponseEntity.ok(response);
        return null;
    }

    // PATCH /guest/orders/{orderId} (비회원 주문 취소)
    @PatchMapping("/{orderId}")
    public ResponseEntity<Void> cancelGuestOrder(@PathVariable Long orderId,
                                                           @RequestParam("password") String password) {
//        OrderResponseDto response = orderService.cancelGuestOrder(orderId, password);
//        return ResponseEntity.ok(response);
        return null;
    }
}