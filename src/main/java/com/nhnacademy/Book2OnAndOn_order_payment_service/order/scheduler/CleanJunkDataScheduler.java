package com.nhnacademy.Book2OnAndOn_order_payment_service.order.scheduler;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanJunkDataScheduler {

    private final OrderRepository orderRepository;
    private final int PAGE_SIZE = 500;

    /*
        매일 정각에 발생하는 주문 정크 데이터들을 삭제합니다
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(
            name = "cleaningOldPendingOrders",
            lockAtLeastFor = "PT10S",
            lockAtMostFor = "PT5M"
    )
    @Transactional
    public void cleaningJunkOrderData(){
        log.info("주문 정크 데이터 삭제 스케줄러 시작");

        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(30);

        int deletedCount = 0;

        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("orderDateTime").ascending());

        Page<Order> page;

        do{
            page = orderRepository.findAllByOrderStatusAndOrderDateTimeBefore(OrderStatus.PENDING, thresholdTime, pageable);

            page.forEach(order -> {
                orderRepository.delete(order);
            });
        }while (page.hasNext());

    }
}
