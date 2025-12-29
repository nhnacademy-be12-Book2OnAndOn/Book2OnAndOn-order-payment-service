package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.GuestOrder;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.util.AesUtils;
import java.time.LocalDateTime;
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
        "spring.cloud.config.import-check.enabled=false",
        "spring.config.import=optional:configserver:"
})
class GuestOrderRepositoryTest {

    @Autowired
    private GuestOrderRepository guestOrderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("주문 번호로 비회원 주문 정보를 조회한다.")
    void findByOrder_OrderNumberTest() {
        // 1. Order 엔티티 생성 및 저장 (연관 관계를 위해 필요)
        Order order = Order.builder()
                .orderNumber("GUEST-20251226")
                .orderTitle("비회원 주문 테스트")
                .totalAmount(50000)
                .orderStatus(OrderStatus.PENDING)
                .orderDateTime(LocalDateTime.now())
                .build();
        entityManager.persist(order);

        // 2. GuestOrder 엔티티 생성 (엔티티의 필드 구성에 맞춤)
        GuestOrder guestOrder = GuestOrder.builder()
                .guestName("홍길동")
                .guestPhoneNumber("01012345678")
                .guestPassword("password123!") // 실제로는 인코딩된 패스워드가 들어가야 함
                .order(order)
                .build();

        guestOrderRepository.save(guestOrder);
        entityManager.flush();
        entityManager.clear();

        // 3. 테스트 실행
        Optional<GuestOrder> result = guestOrderRepository.findByOrder_OrderNumber("GUEST-20251226");

        // 4. 검증
        assertThat(result).isPresent();
        assertThat(result.get().getGuestName()).isEqualTo("홍길동");
        assertThat(result.get().getGuestPhoneNumber()).isEqualTo("01012345678");
        assertThat(result.get().getOrder().getOrderNumber().trim()).isEqualTo("GUEST-20251226");
    }
}