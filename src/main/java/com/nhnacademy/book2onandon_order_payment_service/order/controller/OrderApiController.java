package com.nhnacademy.book2onandon_order_payment_service.order.controller;

import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderApiService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class OrderApiController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final OrderApiService orderApiService;

    @GetMapping("/orders/check-purchase/{bookId}")
    public ResponseEntity<Boolean> hasPurchasedBook(@RequestHeader(USER_ID_HEADER) Long userId,
                                                    @PathVariable("bookId") Long bookId){
        log.info("GET /order/check-purchase/{} 호출 : 유저 책 구매 여부 반환", bookId);
        Boolean hasPurchased = orderApiService.existsPurchase(userId, bookId);
        return ResponseEntity.ok(hasPurchased);
    }

    @GetMapping("/orders/bestsellers")
    public ResponseEntity<List<Long>> getBestSellers(@RequestParam("period") String period){
        List<Long> bestSellerIds = orderApiService.getBestSellers(period);
        return ResponseEntity.ok(bestSellerIds);
    }

    @GetMapping("/orders/users/{userId}/net-amount")
    public ResponseEntity<Long> getNetOrderAmount(@PathVariable("userId") Long userId,
                                                  @RequestParam("from") LocalDate fromDate,
                                                  @RequestParam("to") LocalDate toDate){
        log.info("GET /orders/users/{}/net-amount 호출 : {} ~ {} 기간의 순수 주문액 총합 반환", userId, fromDate, toDate);
        Long sumOrderItemAmount = orderApiService.calculateTotalOrderAmountForUserBetweenDates(userId, fromDate, toDate);
        return ResponseEntity.ok(sumOrderItemAmount);
    }

    // 주문 실패시
    @PatchMapping("/payment/{orderNumber}/rollback")
    // orderNumber, memberCouponId, userId, point, orderId
    public ResponseEntity<Void> rollbackResource(@PathVariable("orderNumber") String orderNumber){

        log.info("PATCH /orders/rollback 주문번호 : {}", orderNumber);
        orderApiService.rollback(orderNumber);
        log.info("주문 롤백 성공");
        return ResponseEntity.noContent().build();
    }
}
