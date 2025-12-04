package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    //추가
    boolean existsByOrder_OrderId(Long orderId);

    // 변경 분할 배송X
    Optional<Delivery> findByOrder_OrderId(Long orderId);

    // 상태별 페이징 조회 (관리자용)
    Page<Delivery> findAllByOrder_OrderStatus(OrderStatus orderStatus, Pageable pageable);

    List<Delivery> findAllByOrder_OrderStatus(OrderStatus orderStatus);
}

