package com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.wrapping;
// ... (생략: import)

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WrappingPaperRepository extends JpaRepository<WrappingPaper, Long> {
}