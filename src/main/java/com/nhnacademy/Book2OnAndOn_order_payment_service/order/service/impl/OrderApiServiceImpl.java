package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderApiService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderApiServiceImpl implements OrderApiService {

    private final OrderRepository orderRepository;
    private final Pageable TOP_10 = PageRequest.of(0, 10);
    private final OrderStatus ORDER_STATUS_DELIVERED = OrderStatus.DELIVERED;
    private final OrderItemStatus ORDER_ITEM_STATUS_DELIVERED = OrderItemStatus.DELIVERED;

    @Transactional(readOnly = true)
    @Override
    public Boolean existsPurchase(Long userId, Long bookId) {
        return orderRepository.existsPurchase(userId, bookId, ORDER_ITEM_STATUS_DELIVERED);
    }

    @Override
    public List<Long> getBestSellers(String period) {
        LocalDate now = LocalDate.now();
        LocalDate start;
        LocalDate end;
        // 일일
        if ("DAILY".equals(period)){
            start = now.minusDays(1);
            end = start;
            return orderRepository.findTopBestSellerBookIds(start, end, ORDER_STATUS_DELIVERED, ORDER_ITEM_STATUS_DELIVERED, TOP_10);
        }

        // 주간
        if("WEEKLY".equals(period)){
            start = now.minusWeeks(1);
            end = now.minusDays(1);
            return orderRepository.findTopBestSellerBookIds(start, end, ORDER_STATUS_DELIVERED, ORDER_ITEM_STATUS_DELIVERED, TOP_10);
        }
        return List.of();
    }

    // 유저
    @Transactional(readOnly = true)
    @Override
    public Long calculateTotalOrderAmountForUserBetweenDates(Long userId, LocalDate fromDate, LocalDate toDate) {
        return orderRepository.sumTotalItemAmountByUserIdAndOrderDateTimeBetween(userId, fromDate, toDate)
                .orElse(0L);
    }
}
