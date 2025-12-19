package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.ReserveBookRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.StockDecreaseRequest;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

// OrderServiceClient 인터페이스 (Book Service의 구매 검증 API 호출용)
@FeignClient(name = "book-service") // ⬅️ Book Service로의 Feign Client
public interface BookServiceClient {
    // 주문에 필요한 도서 정보 목록 조회
    @GetMapping("/internal/books")
    List<BookOrderResponse> getBooksForOrder(@RequestParam("bookIds") List<Long> bookIds);

    // 재고 선점 요청 (임시 주문 생성 전 호출)
    @PatchMapping("/internal/books/stock/reserve")
    void reserveStock(@RequestBody ReserveBookRequestDto request);

    // 재고 복구 요청
    @PatchMapping("/internal/books/stock/release")
    void releaseStock(@RequestParam("orderNumber") String orderNumber);

    // 재고 차감 확정 요청
    @PatchMapping("/internal/books/stock/confirm")
    void confirmStock(@RequestParam("orderNumber") String orderNumber);

    // 재고 차감 요청 (주문 성공 시 호출)
    @PatchMapping("/internal/books/stock/decrease")
    void decreaseStock(@RequestBody List<StockDecreaseRequest> request);

    //재고 증감 요청
    @PatchMapping("/internal/books/stock/increase")
    void increaseStock(@RequestBody List<StockDecreaseRequest> request);
}