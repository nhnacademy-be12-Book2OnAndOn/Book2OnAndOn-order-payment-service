package com.nhnacademy.Book2OnAndOn_order_payment_service.delivery.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.Delivery;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class DeliveryRepositoryTest {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    // --- Helper Method ---
    private Order createOrder(String orderNumber, OrderStatus status) {
        Order order = Order.builder()
                .userId(1L)
                .orderNumber(orderNumber) // 유니크 테스트를 위해 파라미터로 받음
                .orderDateTime(LocalDateTime.now())
                .orderStatus(status)
                .totalAmount(10000)
                .build();
        return entityManager.persist(order);
    }

    // ==========================================
    // 1. 성공 케이스 (Success)
    // ==========================================

    @Test
    @DisplayName("[성공] 주문 ID로 배송 존재 여부 확인 - 존재함")
    void existsByOrder_OrderId_True() {
        // given
        Order order = createOrder(UUID.randomUUID().toString().substring(0, 12), OrderStatus.COMPLETED);
        Delivery delivery = new Delivery(order);
        entityManager.persist(delivery);

        // when
        boolean exists = deliveryRepository.existsByOrder_OrderId(order.getOrderId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("[성공] 주문 ID로 배송 조회 - 조회됨")
    void findByOrder_OrderId_Found() {
        // given
        Order order = createOrder(UUID.randomUUID().toString().substring(0, 12), OrderStatus.COMPLETED);
        Delivery delivery = new Delivery(order);
        entityManager.persist(delivery);

        // when
        Optional<Delivery> found = deliveryRepository.findByOrder_OrderId(order.getOrderId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(delivery);
    }

    @Test
    @DisplayName("[성공] 주문 상태별 배송 목록 조회 (Paging)")
    void findAllByOrder_OrderStatus_Pageable() {
        // given
        Order order1 = createOrder("ORDER-001", OrderStatus.SHIPPING);
        entityManager.persist(new Delivery(order1));

        Order order2 = createOrder("ORDER-002", OrderStatus.PREPARING);
        entityManager.persist(new Delivery(order2));

        // when
        Page<Delivery> result = deliveryRepository.findAllByOrder_OrderStatus(
                OrderStatus.SHIPPING, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrder().getOrderNumber()).isEqualTo("ORDER-001");
    }

    // 실패 케이스 (Fail / Not Found)
    @Test
    @DisplayName("[실패] 주문 ID로 배송 존재 여부 확인 - 존재하지 않음")
    void existsByOrder_OrderId_False() {
        // given

        // when
        boolean exists = deliveryRepository.existsByOrder_OrderId(99999L);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("[실패] 주문 ID로 배송 조회 - 결과 없음")
    void findByOrder_OrderId_NotFound() {
        // given
        Order order = createOrder(UUID.randomUUID().toString().substring(0, 12), OrderStatus.COMPLETED);
        // Order는 있지만 Delivery는 생성 안 함

        // when
        Optional<Delivery> found = deliveryRepository.findByOrder_OrderId(order.getOrderId());

        // then
        assertThat(found).isEmpty();
    }


    // 예외 케이스 (Exception / Constraint)
    @Test
    @DisplayName("[예외] 필수 필드(@NotNull) 누락 시 저장 실패")
    void save_Fail_NotNullConstraint() {
        // given
        // userId는 @NotNull임. 이를 누락하고 생성 시도.
        Order invalidOrder = Order.builder()
                .userId(null) // 필수값 누락
                .orderNumber("FAIL-ORDER")
                .orderDateTime(LocalDateTime.now())
                .orderStatus(OrderStatus.COMPLETED)
                .totalAmount(1000)
                .build();

        // when & then
        // JPA validation은 flush 시점에 발생
        assertThatThrownBy(() -> {
            entityManager.persist(invalidOrder);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("[예외] 주문번호(Unique) 중복 저장 시 실패")
    void save_Fail_UniqueConstraint() {
        // given
        String duplicateOrderNumber = "DUPLICATE123";

        // 첫 번째 주문 저장 (성공)
        createOrder(duplicateOrderNumber, OrderStatus.COMPLETED);

        // 두 번째 주문 저장 (같은 주문번호)
        Order duplicateOrder = Order.builder()
                .userId(2L)
                .orderNumber(duplicateOrderNumber) // 중복!
                .orderDateTime(LocalDateTime.now())
                .orderStatus(OrderStatus.SHIPPING)
                .totalAmount(2000)
                .build();

        assertThatThrownBy(() -> {
            orderRepository.saveAndFlush(duplicateOrder);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}