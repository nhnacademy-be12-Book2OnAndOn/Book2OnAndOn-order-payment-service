package com.nhnacademy.Book2OnAndOn_order_payment_service.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.ReserveBookRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.StockDecreaseRequest;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // bookId 리스트를 받아서, 각 bookId에 대한 스냅샷 정보(가격/제목/재고 등)를 반환
    // (응답 전체(JSON) 캐싱 방법은 서버 부하는 최소일지 몰라도 도서 가격/재고 변경 시 캐시 무효화 지옥...)
    @PostMapping(value = "/internal/books/bulk", consumes = "application/json")
    Map<Long, BookSnapshot> getBookSnapshots(@RequestBody List<Long> bookIds);

    @Getter
    @Setter
    class BookSnapshot {
        private Long bookId;
        private String title;
        private String thumbnailUrl;
        private int originalPrice;
        private int salePrice;
        private int stockCount;
        private boolean saleEnded; // 판매 종료 여부
        private boolean deleted; // 관리자에 의해 삭제된 상품
    }
}
