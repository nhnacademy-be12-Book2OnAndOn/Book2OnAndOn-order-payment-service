package com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryAddress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    Optional<DeliveryAddress> findByOrder_OrderId(Long orderId);
}
