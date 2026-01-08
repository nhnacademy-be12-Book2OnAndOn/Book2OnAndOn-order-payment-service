package com.nhnacademy.book2onandon_order_payment_service.order.service.impl;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderApiService;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.book2onandon_order_payment_service.order.service.OrderTransactionService;
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
    private final GuestOrderRepository guestOrderRepository;

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
    public void rollback(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Not Found Order : " + orderNumber));


        if(order.getOrderStatus() == OrderStatus.PENDING){
            orderResourceManager.releaseResources(orderNumber, order.getUserId(), order.getPointDiscount(), order.getOrderId());
        }

        orderTransactionService.changeStatusOrder(order, false);
    }
}
