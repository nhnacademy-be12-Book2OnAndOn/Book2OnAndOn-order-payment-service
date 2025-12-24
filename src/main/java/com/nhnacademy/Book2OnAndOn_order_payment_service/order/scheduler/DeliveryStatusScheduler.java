package com.nhnacademy.Book2OnAndOn_order_payment_service.order.scheduler;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.SmartDeliveryClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryStatusScheduler {

    private final DeliveryRepository deliveryRepository;
    private final SmartDeliveryClient smartDeliveryClient;

    // 1시간마다 실행 (배송 상태 확인)
    @Scheduled(cron = "0 0 * * * *") 
    @SchedulerLock(
            name = "delivery_status_check_task",
            lockAtLeastFor = "1m",
            lockAtMostFor = "20m"
    )
    @Transactional
    public void checkDeliveryStatus() {
        log.info("배송 상태 확인 스케줄러 시작");

        // 배송 중(SHIPPING) 상태인 건들만 조회
        List<Delivery> shippingDeliveries = deliveryRepository.findAllByOrder_OrderStatus(OrderStatus.SHIPPING);

        int updatedCount = 0;

        for (Delivery delivery : shippingDeliveries) {
            try {

                boolean isComplete = smartDeliveryClient.isDeliveryCompleted(
                        delivery.getDeliveryCompany().getCode(),
                        delivery.getWaybill()
                );

                if (isComplete) {
                    delivery.getOrder().updateStatus(OrderStatus.DELIVERED);
                    updatedCount++;
                    log.info("배송 완료 처리: OrderID={}, Waybill={}", delivery.getOrder().getOrderId(), delivery.getWaybill());
                }

            } catch (Exception e) {
                log.warn("배송 조회 실패 (Skip): DeliveryId={}, Error={}", delivery.getDeliveryId(), e.getMessage());
            }
        }

        log.info("스케줄러 종료. 총 {}건 배송 완료 처리됨.", updatedCount);
    }
}