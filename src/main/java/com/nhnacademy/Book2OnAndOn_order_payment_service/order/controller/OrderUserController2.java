package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelRequestDto2;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderUserController2 {

    private final OrderService2 orderService;
    private static final String USER_ID_HEADER = "X-User-Id";

    // 장바구니 혹은 바로구매시 준비할 데이터 (책 정보, 회원 배송지 정보)
    @GetMapping("/prepare")
    public ResponseEntity<OrderPrepareResponseDto> getOrderPrepare(@RequestHeader(USER_ID_HEADER) Long userId,
                                                                 @RequestBody OrderPrepareRequestDto req){
        log.info("GET /orders/prepare 호출 : 주문시 필요한 데이터 반환 (회원 아이디 : {})", userId);

        // 회원 주문 로직
        OrderPrepareResponseDto orderSheetResponseDto = orderService.prepareOrder(userId, req);

        return ResponseEntity.ok(orderSheetResponseDto);
    }

    @PostMapping
    public ResponseEntity<OrderCreateResponseDto> createPreOrder(@RequestHeader(USER_ID_HEADER) Long userId,
                                                                 @RequestBody OrderCreateRequestDto req){
        log.info("POST /orders 호출 : 사전 주문 데이터 생성");
        OrderCreateResponseDto orderResponseDto = orderService.createOrder(userId, req);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponseDto);
    }

    // 주문조회 리스트 반환
    // 나의 myOrderInfo
    // /users/me/orders
    // /orders post
    // /orders/order-number
    @GetMapping("/my-order")
    public ResponseEntity<Page<OrderSimpleDto>> getOrderList(@RequestHeader(USER_ID_HEADER) Long userId,
                                                             @PageableDefault(size = 20, sort = "orderDateTime", direction = Sort.Direction.DESC)
                                                             Pageable pageable){
        log.info("GET /order 호출 : 주문 리스트 데이터 반환");
        Page<OrderSimpleDto> orderSimpleDtoPage = orderService.getOrderList(userId, pageable);
        return ResponseEntity.ok(orderSimpleDtoPage);
    }

    // TODO GET OrderResponseDto
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDetailResponseDto> getOrderDetail(@RequestHeader(USER_ID_HEADER) Long userId,
                                                                 @PathVariable("orderNumber") String orderNumber){
        log.info("GET /order/{} 호출 : 주문 상세 데이터 반환" , orderNumber);
        OrderDetailResponseDto orderResponseDto = orderService.getOrderDetail(userId, orderNumber);

        return ResponseEntity.ok(orderResponseDto);
    }

//    // TODO 주문 취소 구현 -> payment 서비스에서 수행
//    @PatchMapping("/{orderNumber}/cancel")
//    public ResponseEntity<OrderResponseDto> cancelOrder(@RequestHeader(USER_ID_HEADER) Long userId,
//                                                        @PathVariable("orderNumber") String orderNumber,
//                                                        @RequestBody OrderCancelRequestDto2 req){
//        log.info("PATCH /order/cancel/{} 호출 : 주문 취소", orderNumber);
//
//        OrderResponseDto orderResponseDto = orderService.cancelOrder(userId, orderNumber, req);
//        return null;
//    }


    /*
        API 전용
     */

    @GetMapping("/check-purchase/{bookId}")
    public ResponseEntity<Boolean> hasPurchasedBook(@RequestHeader(USER_ID_HEADER) Long userId,
                                                    @PathVariable("bookId") Long bookId){
        log.info("GET /order/check-purchase/{} 호출 : 유저 책 구매 여부 반환", bookId);
        Boolean hasPurchased = orderService.existsPurchase(userId, bookId);
        return ResponseEntity.ok(hasPurchased);
    }
}
