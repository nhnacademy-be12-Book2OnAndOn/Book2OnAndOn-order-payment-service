package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder_OrderId(Long orderId);
}
