//package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.config;
//
//import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.client.BookServiceClient;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.springframework.context.annotation.Profile;
//
//@Configuration
//public class StubBookServiceClientConfig {
//
//    @Bean
//    public BookServiceClient bookServiceClient() {
//        return new BookServiceClient() {
//
//            @Override
//            public Map<Long, BookSnapshot> getBookSnapshots(List<Long> bookIds) {
//
//                Map<Long, BookSnapshot> result = new HashMap<>();
//
//                for (Long id : bookIds) {
//                    BookSnapshot snapshot = new BookSnapshot();
//                    snapshot.setBookId(id);
//                    snapshot.setTitle("Dummy Book " + id);
//                    snapshot.setThumbnailUrl("https://dummy.example.com/" + id + ".png");
//                    snapshot.setOriginalPrice(15000);
//                    snapshot.setPrice(12000);
//                    snapshot.setStockCount(100); // 넉넉한 재고
//                    snapshot.setSaleEnded(false);
//                    snapshot.setDeleted(false);
//                    snapshot.setHidden(false);
//
//                    result.put(id, snapshot);
//                }
//
//                return result;
//            }
//        };
//    }
//}
