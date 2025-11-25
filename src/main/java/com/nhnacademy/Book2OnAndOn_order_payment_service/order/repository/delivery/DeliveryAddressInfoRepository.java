package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddressInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAddressInfoRepository extends JpaRepository<DeliveryAddressInfo, Long> {
}
