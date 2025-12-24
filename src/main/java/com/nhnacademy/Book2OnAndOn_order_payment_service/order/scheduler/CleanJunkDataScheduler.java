package com.nhnacademy.Book2OnAndOn_order_payment_service.order.scheduler;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanJunkDataScheduler {

    private final OrderService orderService;
    private final int DELETE_SIZE = 1000;

    /*
        매일 정각에 발생하는 주문 정크 데이터들을 삭제합니다
        대용량 데이터를 생각해
     */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(
            name = "cleaningOldPendingOrders",
            lockAtLeastFor = "PT10S",
            lockAtMostFor = "PT5M"
    )
    @Modifying
    public void cleaningJunkOrderData(){
        log.info("주문 정크 데이터 삭제 스케줄러 시작");

        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(30);

        int totalDeleted = 0;
        Long lastId = 0L;
        int count = 0;
        while (count < 5){
            List<Long> ids = orderService.findNextBatch(thresholdTime, lastId, DELETE_SIZE);

            if(ids.isEmpty()){
                break;
            }

            try{
                int deletedCount = orderService.deleteJunkOrder(ids);
                totalDeleted += deletedCount;
                log.info("Batch 삭제 완료, 삭제건수 : {}", deletedCount);
            } catch (Exception e) {
                log.error("Batch 삭제 중 오류 발생, 스킵 처리 : {}", e.getMessage());
                count++;
                log.info("실패 카운트 : {}회 (5회 넘을시 종료)", count);
            }

            // id 갱신
            lastId = ids.getLast();
        }
        log.info("주문 정크 데이터 삭제 스케줄러 종료, 총 삭제건수 : {}", totalDeleted);
    }
}
