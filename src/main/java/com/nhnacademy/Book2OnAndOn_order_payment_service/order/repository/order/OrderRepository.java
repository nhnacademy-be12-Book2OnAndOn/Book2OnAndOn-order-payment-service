package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.*;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o.totalAmount FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Integer> getAmount(String orderNumber);
}

