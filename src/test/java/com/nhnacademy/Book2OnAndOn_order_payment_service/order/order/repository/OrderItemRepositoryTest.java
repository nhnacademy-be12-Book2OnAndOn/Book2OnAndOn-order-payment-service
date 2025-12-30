package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.repository;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.util.AesUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(AesUtils.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "encryption.secret-key=12345678901234567890123456789012"
})
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("주문 ID로 모든 주문 항목 리스트를 조회한다.")
    void findByOrder_OrderIdTest() {
        Order order = Order.builder()
                .orderNumber("ORD-20251226")
                .orderTitle("테스트 주문")
                .totalAmount(30000)
                .orderStatus(OrderStatus.PENDING)
                .orderDateTime(LocalDateTime.now())
                .build();
        entityManager.persist(order);

        OrderItem item1 = OrderItem.builder()
                .bookId(1L)
                .unitPrice(15000)
                .orderItemQuantity(1)
                .isWrapped(false)
                .order(order) // @NotNull 필드
                .build();

        OrderItem item2 = OrderItem.builder()
                .bookId(2L)
                .unitPrice(15000)
                .orderItemQuantity(1)
                .isWrapped(false)
                .order(order)
                .build();

        orderItemRepository.saveAll(List.of(item1, item2));
        entityManager.flush();
        entityManager.clear();

        List<OrderItem> items = orderItemRepository.findByOrder_OrderId(order.getOrderId());

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getUnitPrice()).isEqualTo(15000);
    }

    @Test
    @DisplayName("비관적 잠금을 적용하여 항목을 조회한다.")
    void findByIdForUpdateTest() {
        Order order = Order.builder()
                .orderNumber("ORD-LOCK")
                .orderTitle("잠금 테스트")
                .totalAmount(10000)
                .orderStatus(OrderStatus.PENDING)
                .orderDateTime(LocalDateTime.now())
                .build();
        entityManager.persist(order);

        OrderItem item = OrderItem.builder()
                .bookId(100L)
                .unitPrice(10000)
                .orderItemQuantity(1)
                .order(order)
                .build();
        OrderItem saved = orderItemRepository.save(item);

        entityManager.flush();
        entityManager.clear();

        Optional<OrderItem> result = orderItemRepository.findByIdForUpdate(saved.getOrderItemId());

        assertThat(result).isPresent();
        assertThat(result.get().getBookId()).isEqualTo(100L);
    }
}