package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryItemRepository extends JpaRepository<DeliveryItem, Long> {
}
