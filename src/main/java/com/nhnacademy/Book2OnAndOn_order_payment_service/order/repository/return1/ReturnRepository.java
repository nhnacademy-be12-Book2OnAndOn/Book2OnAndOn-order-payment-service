package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.return1;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1.Return;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {
    // 특정 주문에 대한 모든 반품 내역 조회(재반품 고려해 List로)
    List<Return> findByOrder_OrderId(Long orderId);
}

