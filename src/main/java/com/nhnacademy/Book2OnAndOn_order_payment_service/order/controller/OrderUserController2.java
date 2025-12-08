package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService2;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderUserController2 {

    private final OrderService2 orderService;

    // 사전 주문 데이터 생성
    @PostMapping
    public ResponseEntity<OrderSimpleDto> createPreOrder(@RequestHeader(value = "X-USER-ID") Long userId, OrderCreateRequestDto req){
        log.info("POST /order 호출 : 사전 주문 데이터 생성");
        OrderSimpleDto orderSimpleDto = orderService.createOrder(userId, req);
        return ResponseEntity.ok(orderSimpleDto);
    }

    // TODO GET List<OrderSimpleDto>
    // 주문조회 리스트 반환
    @GetMapping
    public ResponseEntity<Page<OrderSimpleDto>> getOrderList(@RequestHeader(value = "X-USER-ID") Long userId,
                                                             @PageableDefault(size = 20, sort = "orderDateTime", direction = Sort.Direction.DESC)
                                                             Pageable pageable){
        log.info("GET /order 호출 : 주문 리스트 데이터 반환");
        Page<OrderSimpleDto> orderSimpleDtoPage = orderService.getOrderList(userId, pageable);
        return ResponseEntity.ok(orderSimpleDtoPage);
    }

    // TODO GET OrderResponseDto
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponseDto> getOrderDetail(@RequestHeader(value = "X-USER-ID") Long userId,
                                                           @PathVariable("orderNumber") String orderNumber){
        log.info("GET /order/{} 호출 : 주문 상세 데이터 반환" , orderNumber);
        OrderResponseDto orderResponseDto = orderService.getOrderDetail(userId, orderNumber);

        return ResponseEntity.ok(orderResponseDto);
    }

    // TODO PATCH OrderResponseDto 주문 임시 저장 타이밍을 언제로?


    // TODO 유저 책 구매 여부
    @GetMapping("/check-purchase/{bookId}")
    public ResponseEntity<Boolean> hasPurchasedBook(@RequestHeader("X-USER-ID") Long userId,
                                                    @PathVariable("bookId") Long bookId){
        log.info("GET /order/check-purchase/{} 호출 : 유저 책 구매 여부 반환", bookId);
        Boolean hasPurchased = orderService.existsPurchase(userId, bookId);
        return ResponseEntity.ok(hasPurchased);
    }
}
