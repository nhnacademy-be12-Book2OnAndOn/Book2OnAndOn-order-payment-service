package com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryPolicyRepository extends JpaRepository<DeliveryPolicy, Long> {

    Optional<DeliveryPolicy> findFirstByDeliveryPolicyName(String deliveryPolicyName);
}
