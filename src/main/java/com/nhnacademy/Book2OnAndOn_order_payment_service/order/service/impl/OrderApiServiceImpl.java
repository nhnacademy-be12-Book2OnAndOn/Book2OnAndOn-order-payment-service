package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderRollbackDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderApiService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final OrderTransactionService orderTransactionService;
    private final OrderResourceManager orderResourceManager;

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
        log.info(">>> [OrderService] getBestSellers 진입. period: [{}]", period);
        LocalDate now = LocalDate.now();
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        // 일일 (어제 00:00:00 ~ 어제 23:59:59)
        if ("DAILY".equals(period)) {
            LocalDate yesterday = now.minusDays(1);
            startDateTime = yesterday.atStartOfDay();
            endDateTime = yesterday.atTime(LocalTime.MAX);
            return orderRepository.findTopBestSellerBookIds(
                    startDateTime, endDateTime, ORDER_STATUS_DELIVERED, ORDER_ITEM_STATUS_DELIVERED, TOP_10);
        }

        // 주간 (7일 전 00:00:00 ~ 어제 23:59:59)
        if ("WEEKLY".equals(period)) {
            startDateTime = now.minusWeeks(1).atStartOfDay();
            endDateTime = now.minusDays(1).atTime(LocalTime.MAX);
            return orderRepository.findTopBestSellerBookIds(
                    startDateTime, endDateTime, ORDER_STATUS_DELIVERED, ORDER_ITEM_STATUS_DELIVERED, TOP_10);
        }
        return List.of();
    }

    // 유저
    @Transactional(readOnly = true)
    @Override
    public Long calculateTotalOrderAmountForUserBetweenDates(Long userId, LocalDate fromDate, LocalDate toDate) {

        // LocalDate -> LocalDateTime 변환
        LocalDateTime fromDateTime = fromDate.atStartOfDay(); // 00:00:00
        LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX); // 23:59:59.999999999
        return orderRepository.sumTotalItemAmountByUserIdAndOrderDateTimeBetween(userId, fromDateTime, toDateTime)
                .orElse(0L);
    }

    @Override
    @Transactional
    public void rollback(Long userId, OrderRollbackDto req) {
        // 회원 / 비회원 분기 처리
        if(userId == null){
            // TODO 비회원 작업
        }

        Order order = orderTransactionService.validateOrderExistence(userId, req.orderNumber());

        orderResourceManager.releaseResources(req.orderNumber(), userId, order.getPointDiscount(), order.getOrderId());

        order.setOrderStatus(OrderStatus.CANCELED);

        for (OrderItem orderItem : order.getOrderItems()) {
            orderItem.setOrderItemStatus(OrderItemStatus.ORDER_CANCELED);
        }
    }
}
