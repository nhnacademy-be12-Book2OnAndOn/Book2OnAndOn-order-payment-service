package com.nhnacademy.book2onandon_order_payment_service.order.exception;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import java.util.Set;
import java.util.stream.Collectors;

public class RefundNotCancelableException extends RuntimeException {

    private static final String MESSAGE =
            "현재 상태에서는 반품 취소가 불가합니다. refundId=%s, status=%s, cancelable=%s";

    public RefundNotCancelableException(Long refundId, RefundStatus currentStatus, Set<RefundStatus> cancelableStatuses) {
        super(String.format(
                MESSAGE,
                refundId,
                currentStatus,
                cancelableStatuses.stream().map(Enum::name).collect(Collectors.joining(","))
        ));
    }

    // CANCELABLE_STATUSES가 EnumSet/List이면 호출 편하게 오버로드
    public RefundNotCancelableException(Long refundId, RefundStatus currentStatus, java.util.Collection<RefundStatus> cancelableStatuses) {
        this(refundId, currentStatus, Set.copyOf(cancelableStatuses));
    }
}
