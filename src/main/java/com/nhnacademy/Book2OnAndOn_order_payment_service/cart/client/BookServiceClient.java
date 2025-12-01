package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.client;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

// 모든 도서 정보(가격/제목/재고/상태)를 외부 서비스(book-service)에서 가져오기 위한 FeignClient
@Component
@FeignClient(name = "BOOK-SERVICE", url = "${book.service.url}")
public interface BookServiceClient {

    // bookId 리스트를 받아서, 각 bookId에 대한 스냅샷 정보(가격/제목/재고 등)를 반환
    // (응답 전체(JSON) 캐싱 방법은 서버 부하는 최소일지 몰라도 도서 가격/재고 변경 시 캐시 무효화 지옥...)
    @PostMapping("/api/books/bulk")
    Map<Long, BookSnapshot> getBookSnapshots(List<Long> bookIds);
     // <key : value> -> 한 번의 호출로 여러 건의 데이터 호출 가능

    @Getter
    @Setter
    class BookSnapshot {
        private Long bookId;
        private String title;
        private String thumbnailUrl;
        private int originalPrice;
        private int price;
        private int stockCount;
        private boolean saleEnded;
        private boolean deleted;
        private boolean hidden;
    }
}
//=> 스냅샷 결과를 받기 위해서는 book의 service에
//BookSnapshot from(Book book) {
//    BookSnapshot s = new BookSnapshot();
//    s.setBookId(book.getId());
//    s.setTitle(book.getTitle());
//    s.setThumbnailUrl(book.getImages().isEmpty() ? null : book.getImages().get(0).getUrl());
//    s.setOriginalPrice(book.getPriceStandard().intValue());
//    s.setPrice(book.getPriceSales() != null ? book.getPriceSales().intValue() : book.getPriceStandard().intValue());
//    s.setStockCount(book.getStockCount() != null ? book.getStockCount() : 0);
//
//    // 상태 값은 BookStatus에서 해석
//    s.setSaleEnded(book.getStatus() == BookStatus.SALE_ENDED);
//    s.setDeleted(book.getStatus() == BookStatus.DELETED);
//    s.setHidden(book.getStatus() == BookStatus.HIDDEN);
//
//    return s;
//}
//작성해둬야 함

//BOOK service에 아래 필요
//1) BookSnapshotResponse DTO (노출 모델)
//@Getter
//@Setter
//public class BookSnapshotResponse {
//    private Long bookId;
//    private String title;
//    private String thumbnailUrl;
//    private int originalPrice;
//    private int price;
//    private int stockCount;
//    private boolean saleEnded;
//    private boolean deleted;
//    private boolean hidden;
//}

//2) BookSnapshotMapper (from 메서드 위치)
//@Component
//public class BookSnapshotMapper {
//
//    public BookSnapshotResponse from(Book book) {
//
//        BookSnapshotResponse s = new BookSnapshotResponse();
//        s.setBookId(book.getId());
//        s.setTitle(book.getTitle());
//        s.setThumbnailUrl(
//                book.getImages().isEmpty() ? null : book.getImages().get(0).getUrl()
//        );
//        s.setOriginalPrice(book.getPriceStandard().intValue());
//
//        // 판매가 없으면 정가를 그대로 사용
//        s.setPrice(
//                book.getPriceSales() != null
//                        ? book.getPriceSales().intValue()
//                        : book.getPriceStandard().intValue()
//        );
//
//        s.setStockCount(
//                book.getStockCount() != null ? book.getStockCount() : 0
//        );
//
//        s.setSaleEnded(book.getStatus() == BookStatus.SALE_ENDED);
//        s.setDeleted(book.getStatus() == BookStatus.DELETED);
//        s.setHidden(book.getStatus() == BookStatus.HIDDEN);
//
//        return s;
//    }
//}

//3) BookService(=book-service 내부)에서 스냅샷 만들어 반환
//@Service
//@RequiredArgsConstructor
//public class BookQueryService {
//
//    private final BookRepository bookRepository;
//    private final BookSnapshotMapper snapshotMapper;
//
//    public Map<Long, BookSnapshotResponse> getSnapshots(List<Long> bookIds) {
//
//        List<Book> books = bookRepository.findAllById(bookIds);
//
//        return books.stream()
//                .map(snapshotMapper::from)
//                .collect(Collectors.toMap(
//                        BookSnapshotResponse::getBookId,
//                        s -> s
//                ));
//    }
//}

//4) book-service의 API Controller
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/books")
//public class BookSnapshotApi {
//
//    private final BookQueryService bookQueryService;
//
//    @PostMapping("/bulk")
//    public Map<Long, BookSnapshotResponse> getBooks(@RequestBody List<Long> bookIds) {
//        return bookQueryService.getSnapshots(bookIds);
//    }
//}
