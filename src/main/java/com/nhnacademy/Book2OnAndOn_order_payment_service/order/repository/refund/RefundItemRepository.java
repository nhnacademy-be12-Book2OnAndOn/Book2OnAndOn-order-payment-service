package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.refund;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    interface OrderItemQtyAggregate {
        Long getOrderItemId();
        Integer getQuantity();
    }

    // 완료(환불 완료) 기준으로만 "이미 반품된 수량" 합산
    @Query("""
        select coalesce(sum(ri.refundQuantity), 0)
        from RefundItem ri
        where ri.orderItem.orderItemId = :orderItemId
          and ri.refund.refundStatus = :completedStatus
    """)
    int sumCompletedRefundQuantity(
            @Param("orderItemId") Long orderItemId,
            @Param("completedStatus") RefundStatus completedStatus
    );

    // 진행중(요청~검수중 등) 반품 수량 합산 (동시성/초과 신청 방지용 “예약 수량”)
    @Query("""
        select coalesce(sum(ri.refundQuantity), 0)
        from RefundItem ri
        where ri.orderItem.orderItemId = :orderItemId
          and ri.refund.refundStatus in :activeStatuses
    """)
    int sumActiveRefundQuantity(
            @Param("orderItemId") Long orderItemId,
            @Param("activeStatuses") Collection<RefundStatus> activeStatuses
    );

    // 2. 특정 주문 항목에 대해 '진행 중' 상태의 반품이 존재하는지 여부
    boolean existsByOrderItemOrderItemIdAndRefundRefundStatusIn(Long orderItemId, Collection<RefundStatus> refundStatuses);

    @Query("""
        select oi.orderItemId as orderItemId,
               sum(ri.refundQuantity) as quantity
        from RefundItem ri
        join ri.refund r
        join ri.orderItem oi
        join oi.order o
        where o.orderId = :orderId
          and r.refundStatus = :status
        group by oi.orderItemId
    """)
    List<OrderItemQtyAggregate> sumByOrderIdAndStatus(
            @Param("orderId") Long orderId,
            @Param("status") RefundStatus status
    );

    @Query("""
        select oi.orderItemId as orderItemId,
               sum(ri.refundQuantity) as quantity
        from RefundItem ri
        join ri.refund r
        join ri.orderItem oi
        join oi.order o
        where o.orderId = :orderId
          and r.refundStatus in :statuses
        group by oi.orderItemId
    """)
    List<OrderItemQtyAggregate> sumByOrderIdAndStatuses(
            @Param("orderId") Long orderId,
            @Param("statuses") List<RefundStatus> statuses
    );
}
