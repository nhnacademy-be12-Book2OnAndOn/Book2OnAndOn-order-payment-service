package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.refund;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundItem;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    // 1. 특정 주문 항목(OrderItem)에 대해 현재까지 반품된 수량 합계
    @Query("""
        select coalesce(sum(ri.refundQuantity), 0)
        from RefundItem ri
        where ri.orderItem.orderItemId =: orderItemId
    """)
    int sumReturnedQuantityByOrderItemId(@Param("orderItemId") Long orderItemId);

    // 2. 특정 주문 항목에 대해 '진행 중' 상태의 반품이 존재하는지 여부
    boolean existsByOrderItemOrderItemIdAndRefundRefundStatusIn(Long orderItemId, Collection<Integer> refundStatuses);
}
