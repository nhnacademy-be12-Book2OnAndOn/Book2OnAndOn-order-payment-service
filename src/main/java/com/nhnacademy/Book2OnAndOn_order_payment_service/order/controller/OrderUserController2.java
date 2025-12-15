//package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;
//
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSheetRequestDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSheetResponseDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService2;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
//import java.util.List;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PatchMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestHeader;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@Slf4j
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/order")
//public class OrderUserController2 {
//
//    private final OrderService2 orderService;
//
//    // 장바구니 혹은 바로구매시 준비할 데이터 (책 정보, 회원 배송지 정보, 사용 쿠폰 정보, 포인트 정보)
//    @GetMapping
//    public ResponseEntity<OrderSheetResponseDto> getOrderSheet(@RequestHeader(value = "X-USER-ID") Long userId,
//                                                               @RequestBody OrderSheetRequestDto req){
//        log.info("GET /order 호출 : 주문시 필요한 데이터 반환");
//        OrderSheetResponseDto orderSheetResponseDto = orderService.setOrder(userId, req);
//        return ResponseEntity.ok(orderSheetResponseDto);
//    }
//
//    // 사전 주문 데이터 생성
//    @PostMapping
//    public ResponseEntity<OrderResponseDto> createPreOrder(@RequestHeader(value = "X-USER-ID") Long userId, OrderCreateRequestDto req){
//        log.info("POST /order 호출 : 사전 주문 데이터 생성");
//        OrderResponseDto orderResponseDto = orderService.createOrder(userId, req);
//        return ResponseEntity.ok(orderResponseDto);
//    }
//
//    // TODO GET List<OrderSimpleDto>
//    // 주문조회 리스트 반환
//    @GetMapping
//    public ResponseEntity<Page<OrderSimpleDto>> getOrderList(@RequestHeader(value = "X-USER-ID") Long userId,
//                                                             @PageableDefault(size = 20, sort = "orderDateTime", direction = Sort.Direction.DESC)
//                                                             Pageable pageable){
//        log.info("GET /order 호출 : 주문 리스트 데이터 반환");
//        Page<OrderSimpleDto> orderSimpleDtoPage = orderService.getOrderList(userId, pageable);
//        return ResponseEntity.ok(orderSimpleDtoPage);
//    }
//
//    // TODO GET OrderResponseDto
//    @GetMapping("/{orderNumber}")
//    public ResponseEntity<OrderResponseDto> getOrderDetail(@RequestHeader(value = "X-USER-ID") Long userId,
//                                                           @PathVariable("orderNumber") String orderNumber){
//        log.info("GET /order/{} 호출 : 주문 상세 데이터 반환" , orderNumber);
//        OrderResponseDto orderResponseDto = orderService.getOrderDetail(userId, orderNumber);
//
//        return ResponseEntity.ok(orderResponseDto);
//    }
//
//    @PatchMapping("/cancel/{orderNumber}")
//    public ResponseEntity<OrderResponseDto> cancelOrder(@RequestHeader(value = "X-USER-ID") Long userId,
//                                                        @PathVariable("orderNumber") String orderNumber,
//                                                        @RequestBody CommonCancelRequest req){
//        log.info("PATCH /order/cancel/{} 호출 : 주문 취소", orderNumber);
//        OrderResponseDto orderResponseDto = orderService.cancelOrder(userId, orderNumber, req);
//        return ResponseEntity.ok(orderResponseDto);
//    }
//
//
//    /*
//        API 전용
//     */
//
//    // TODO 유저 책 구매 여부
//    @GetMapping("/check-purchase/{bookId}")
//    public ResponseEntity<Boolean> hasPurchasedBook(@RequestHeader("X-USER-ID") Long userId,
//                                                    @PathVariable("bookId") Long bookId){
//        log.info("GET /order/check-purchase/{} 호출 : 유저 책 구매 여부 반환", bookId);
//        Boolean hasPurchased = orderService.existsPurchase(userId, bookId);
//        return ResponseEntity.ok(hasPurchased);
//    }
//}
