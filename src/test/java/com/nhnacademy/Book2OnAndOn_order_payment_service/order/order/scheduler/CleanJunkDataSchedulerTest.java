package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.scheduler.CleanJunkDataScheduler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CleanJunkDataSchedulerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CleanJunkDataScheduler cleanJunkDataScheduler;

    @Test
    @DisplayName("정크 데이터를 배치 단위로 순회하며 모두 삭제한다")
    void cleaningJunkOrderData_FullSuccess() {
        List<Long> firstBatch = List.of(1L, 2L, 3L);
        List<Long> secondBatch = List.of(4L, 5L);

        given(orderService.findNextBatch(any(), anyLong(), anyInt()))
                .willReturn(firstBatch)
                .willReturn(secondBatch)
                .willReturn(Collections.emptyList());

        given(orderService.deleteJunkOrder(firstBatch)).willReturn(3);
        given(orderService.deleteJunkOrder(secondBatch)).willReturn(2);

        cleanJunkDataScheduler.cleaningJunkOrderData();

        verify(orderService, times(3)).findNextBatch(any(), anyLong(), anyInt());
        verify(orderService, times(2)).deleteJunkOrder(any());
    }

    @Test
    @DisplayName("삭제 도중 예외가 발생해도 실패 카운트를 증가시키며 루프를 계속한다")
    void cleaningJunkOrderData_WithException() {
        List<Long> batchIds = List.of(1L, 2L);
        
        given(orderService.findNextBatch(any(), anyLong(), anyInt()))
                .willReturn(batchIds);
        
        given(orderService.deleteJunkOrder(batchIds))
                .willThrow(new RuntimeException("DB Error"));

        cleanJunkDataScheduler.cleaningJunkOrderData();

        verify(orderService, times(5)).deleteJunkOrder(batchIds);
    }

    @Test
    @DisplayName("삭제할 데이터가 처음부터 없는 경우 즉시 종료한다")
    void cleaningJunkOrderData_NoData() {
        given(orderService.findNextBatch(any(), anyLong(), anyInt()))
                .willReturn(Collections.emptyList());

        cleanJunkDataScheduler.cleaningJunkOrderData();

        verify(orderService, times(1)).findNextBatch(any(), anyLong(), anyInt());
        verify(orderService, times(0)).deleteJunkOrder(any());
    }
}