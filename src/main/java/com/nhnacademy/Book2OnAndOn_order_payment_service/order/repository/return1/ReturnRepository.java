package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.return1;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1.Return;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {
}

