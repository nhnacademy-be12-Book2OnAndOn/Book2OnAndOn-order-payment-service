package com.nhnacademy.book2onandon_order_payment_service.order.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.RefundCompletedEvent;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.RefundNotFoundException;
import com.nhnacademy.book2onandon_order_payment_service.order.listener.RefundCompletedEventListener;
import com.nhnacademy.book2onandon_order_payment_service.order.listener.RefundPointProcessor;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.refund.RefundRepository;
import feign.FeignException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundCompletedEventListenerTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private RefundPointProcessor refundPointProcessor;

    @InjectMocks
    private RefundCompletedEventListener eventListener;

    @Test
    @DisplayName("환불 완료 이벤트 수신 시 포인트 환불 프로세서를 호출한다")
    void handleRefundCompleted_Success() {
        Long refundId = 1L;
        RefundCompletedEvent event = new RefundCompletedEvent(refundId);
        Refund refund = mock(Refund.class);

        given(refundRepository.findById(refundId)).willReturn(Optional.of(refund));
        given(refund.getRefundStatus()).willReturn(RefundStatus.REFUND_COMPLETED);
        given(refund.getRefundId()).willReturn(refundId);

        eventListener.handleRefundCompleted(event);

        verify(refundPointProcessor, times(1)).refundAsPoint(refund);
    }

    @Test
    @DisplayName("반품 내역이 존재하지 않으면 RefundNotFoundException을 던진다")
    void handleRefundCompleted_Fail_NotFound() {
        Long refundId = 999L;
        RefundCompletedEvent event = new RefundCompletedEvent(refundId);

        given(refundRepository.findById(refundId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventListener.handleRefundCompleted(event))
                .isInstanceOf(RefundNotFoundException.class);
    }

    @Test
    @DisplayName("환불 상태가 REFUND_COMPLETED가 아니면 포인트 처리를 하지 않는다")
    void handleRefundCompleted_Fail_InvalidStatus() {
        Long refundId = 1L;
        RefundCompletedEvent event = new RefundCompletedEvent(refundId);
        Refund refund = mock(Refund.class);

        given(refundRepository.findById(refundId)).willReturn(Optional.of(refund));
        given(refund.getRefundStatus()).willReturn(RefundStatus.REQUESTED);

        eventListener.handleRefundCompleted(event);

        verifyNoInteractions(refundPointProcessor);
    }

    @Test
    @DisplayName("포인트 환불 중 FeignException이 발생해도 로그를 남기고 종료한다")
    void handleRefundCompleted_Fail_FeignException() {
        Long refundId = 1L;
        RefundCompletedEvent event = new RefundCompletedEvent(refundId);
        Refund refund = mock(Refund.class);

        given(refundRepository.findById(refundId)).willReturn(Optional.of(refund));
        given(refund.getRefundStatus()).willReturn(RefundStatus.REFUND_COMPLETED);
        given(refund.getRefundId()).willReturn(refundId);
        
        doThrow(mock(FeignException.class)).when(refundPointProcessor).refundAsPoint(refund);

        eventListener.handleRefundCompleted(event);

        verify(refundPointProcessor, times(1)).refundAsPoint(refund);
    }
}