package com.nhnacademy.book2onandon_order_payment_service.order.controller;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestLoginRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestLoginResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest.GuestOrderCreateRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.GuestOrderService;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderService;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/guest/orders")
public class OrderGuestController {

    private final OrderService orderService;
    private final GuestOrderService guestOrderService;
    private static final String GUEST_ID_HEADER = "X-Guest-Id";

    @PostMapping("/login")
    public ResponseEntity<GuestLoginResponseDto> loginGuest(@RequestBody GuestLoginRequestDto requestDto) {

        GuestLoginResponseDto responseDto = guestOrderService.loginGuest(requestDto);

        return ResponseEntity.ok(responseDto);
    }

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

    @PatchMapping("/{orderNumber}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable("orderNumber") String orderNumber,
                                            @RequestHeader(value = "X-Guest-Order-Token", required = false) String guestToken){
        if (guestToken != null) {
            // 비회원 주문 취소
            orderService.cancelGuestOrder(orderNumber, guestToken);
        } else {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        return ResponseEntity.noContent().build();
    }
}