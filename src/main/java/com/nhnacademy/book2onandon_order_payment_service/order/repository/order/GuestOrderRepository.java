package com.nhnacademy.book2onandon_order_payment_service.order.repository.order;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.GuestOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestOrderRepository extends JpaRepository<GuestOrder, Long> {
    Optional<GuestOrder> findByOrder_OrderNumber(String orderNumber);
}
