package com.nhnacademy.Book2OnAndOn_order_payment_service.order.listener;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundCompletedEvent;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.Refund;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.RefundNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.refund.RefundRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.RefundServiceImpl; // 아래에서 제거할 것이므로 의존하지 않는 게 더 좋음
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundCompletedEventListener {

    private final RefundRepository refundRepository;
    private final RefundPointProcessor refundPointProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRefundCompleted(RefundCompletedEvent event) {
        Refund refund = refundRepository.findById(event.refundId())
                .orElseThrow(() -> new RefundNotFoundException(
                        "환불 완료 이벤트 처리 중 반품 내역을 찾을 수 없습니다. id=" + event.refundId()
                ));

        if (refund.getRefundStatus() != RefundStatus.REFUND_COMPLETED) {
            return;
        }

        try {
            refundPointProcessor.refundAsPoint(refund);
            log.info("포인트 환불 성공: refundId={}", refund.getRefundId());
        } catch (FeignException ex) {
            log.error("포인트 환불 실패: refundId={}, message={}", refund.getRefundId(), ex.getMessage(), ex);
        }
    }
}
