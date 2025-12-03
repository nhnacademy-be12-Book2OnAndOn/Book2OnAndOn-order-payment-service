package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.*;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 1. N+1 해결을 위한 상세 조회 (Order, OrderItems, DeliveryAddress를 Fetch Join)
    // DTO 변환 시 사용하므로 이들이 N+1 문제의 핵심
    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems oi JOIN FETCH o.deliveryAddress da WHERE o.orderId = :orderId")
    Optional<Order> findOrderWithDetails(Long orderId);
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // 해당아이디에 해당 주문이 있는지
    boolean existsByOrderNumberAndUserId(String orderNumber, Long userId);
    // 결제 검증용
    @Query("SELECT o.totalAmount FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Integer> findTotalAmount(String orderNumber);
}

