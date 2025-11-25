package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery;
// ... (생략: import)

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    // 특정 주문 ID로 배송 목록을 찾는 메서드를 추가할 수 있습니다. (분할 배송)
    // List<Delivery> findByOrder_OrderId(Long orderId);
}

