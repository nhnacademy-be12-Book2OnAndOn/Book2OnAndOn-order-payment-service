package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder_OrderId(Long orderId);
    long countByWrappingPaper_WrappingPaperId(Long wrappingPaperId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderItemId = :id")
    Optional<OrderItem> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select oi
        from OrderItem oi
        where oi.order.orderId = :orderId
          and oi.orderItemId in :itemIds
    """)
    List<OrderItem> findByOrderIdAndItemIdsForUpdate(
            @Param("orderId") Long orderId,
            @Param("itemIds") List<Long> itemIds
    );
}
