package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    // 사전 주문 데이터 저장
    @PostMapping
    public ResponseEntity<OrderSimpleDto> createPreOrder(@RequestHeader(value = "X-USER-ID", required = true) Long userId){
        log.info("POST /order 호출");
        OrderSimpleDto orderSimpleDto = orderService.createPreOrder(userId);

        return ResponseEntity.ok(orderSimpleDto);
    }

    // TODO GET List<OrderSimpleDto>

    // TODO GET OrderResponseDto

    // TODO DELETE OrderSimpleDto

    // TODO PATCH OrderResponseDto

    // 배치 스케쥴러 사용
    // 정크 데이터 삭제용
}
