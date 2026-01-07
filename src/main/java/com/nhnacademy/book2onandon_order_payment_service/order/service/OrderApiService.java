package com.nhnacademy.book2onandon_order_payment_service.order.service;

import java.time.LocalDate;
import java.util.List;

public interface OrderApiService {
    // -- 도서 --
    Boolean existsPurchase(Long userId, Long bookId);
    List<Long> getBestSellers(String period);
    // -- 유저 --
    Long calculateTotalOrderAmountForUserBetweenDates(Long userId, LocalDate fromDate, LocalDate toDate);

    // 자원 롤백
    void rollback(String orderNumber);
}
