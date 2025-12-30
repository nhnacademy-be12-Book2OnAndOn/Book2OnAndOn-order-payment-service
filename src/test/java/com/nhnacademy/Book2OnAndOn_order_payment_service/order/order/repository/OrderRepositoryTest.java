package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(AesUtils.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "encryption.secret-key=12345678901234567890123456789012"
})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Fetch Join을 통해 주문, 주문항목, 배송지 정보를 한 번에 상세 조회한다.")
    void findOrderWithDetailsTest() {
        Order order = Order.builder()
                .orderNumber("ORD-DETAIL-001")
                .orderStatus(OrderStatus.PENDING)
                .orderTitle("상세 조회 테스트")
                .totalAmount(50000)
                .totalItemAmount(47000)
                .deliveryFee(3000)
                .build();

        OrderItem item = OrderItem.builder()
                .bookId(1L)
                .unitPrice(47000)
                .orderItemQuantity(1)
                .order(order)
                .build();
        order.addOrderItem(List.of(item));

        DeliveryAddress address = DeliveryAddress.builder()
                .recipient("홍길동")
                .deliveryAddress("광주광역시")
                .recipientPhoneNumber("01012345678")
                .order(order)
                .build();
        order.addDeliveryAddress(address);

        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();

        Optional<Order> result = orderRepository.findOrderWithDetails(order.getOrderId());

        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber().trim()).isEqualTo("ORD-DETAIL-001");
        assertThat(result.get().getOrderItems()).hasSize(1);
        assertThat(result.get().getDeliveryAddress().getRecipient()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("사용자 ID로 간단 주문 정보(DTO) 목록을 페이징하여 조회한다.")
    void findAllByUserIdTest() {
        Long testUserId = 100L;
        Order order = Order.builder()
                .userId(testUserId)
                .orderNumber("ORD-USER-100")
                .orderStatus(OrderStatus.COMPLETED)
                .orderTitle("DTO 테스트용 주문")
                .totalAmount(20000)
                .totalItemAmount(20000)
                .build();
        orderRepository.save(order);

        Page<OrderSimpleDto> result = orderRepository.findAllByUserId(testUserId, PageRequest.of(0, 10));

        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent().get(0).getOrderNumber().trim()).isEqualTo("ORD-USER-100");
    }

    @Test
    @DisplayName("주문 상태와 특정 시간을 기준으로 스케줄러용 배치 ID 목록을 조회한다.")
    void findNextBatchTest() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        Order junkOrder = Order.builder()
                .orderNumber("JUNK-001")
                .orderStatus(OrderStatus.PENDING)
                .orderDateTime(LocalDateTime.now().minusDays(2))
                .totalAmount(0)
                .build();
        orderRepository.save(junkOrder);

        List<Long> ids = orderRepository.findNextBatch(0, threshold, 0L, 10);

        assertThat((Iterable<Long>) ids).contains(junkOrder.getOrderId());
    }

    @Test
    @DisplayName("특정 사용자가 특정 도서를 구매했는지 여부를 확인한다.")
    void existsPurchaseTest() {
        Long userId = 50L;
        Long bookId = 999L;
        Order order = Order.builder()
                .userId(userId)
                .orderNumber("PURCHASE-CHECK")
                .orderStatus(OrderStatus.COMPLETED)
                .build();

        OrderItem item = OrderItem.builder()
                .bookId(bookId)
                .unitPrice(10000)
                .orderItemQuantity(1)
                .orderItemStatus(OrderItemStatus.ORDER_COMPLETE)
                .order(order)
                .build();
        order.addOrderItem(List.of(item));

        orderRepository.save(order);

        boolean exists = orderRepository.existsPurchase(userId, bookId, OrderItemStatus.ORDER_COMPLETE);

        assertThat(exists).isTrue();
    }
}