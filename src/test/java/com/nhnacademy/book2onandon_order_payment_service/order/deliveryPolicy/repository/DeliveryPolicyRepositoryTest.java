//package com.nhnacademy.book2onandon_order_payment_service.order.repository.delivery;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryPolicy;
//import com.nhnacademy.book2onandon_order_payment_service.util.AesUtils; // AesUtils 위치 확인 필요
//import java.util.Optional;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//
//@DataJpaTest(properties = {
//        "spring.cloud.config.enabled=false",
//        "spring.config.import-check.enabled=false"
//})
//@ActiveProfiles("test")
//@Import(AesUtils.class)
//class DeliveryPolicyRepositoryTest {
//
//    @Autowired
//    private DeliveryPolicyRepository deliveryPolicyRepository;
//
//    @Autowired
//    private TestEntityManager entityManager;
//
//    @Test
//    @DisplayName("정책 이름으로 첫 번째 배송 정책 조회 성공")
//    void findFirstByDeliveryPolicyName_Success() {
//        DeliveryPolicy policy = new DeliveryPolicy("기본 배송 정책", 3000, 30000);
//        entityManager.persist(policy);
//        entityManager.flush();
//
//        Optional<DeliveryPolicy> result = deliveryPolicyRepository.findFirstByDeliveryPolicyName("기본 배송 정책");
//
//        assertThat(result).isPresent();
//        assertThat(result.get().getDeliveryPolicyName()).isEqualTo("기본 배송 정책");
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 정책 이름 조회 시 빈 Optional 반환 ")
//    void findFirstByDeliveryPolicyName_NotFound() {
//        Optional<DeliveryPolicy> result = deliveryPolicyRepository.findFirstByDeliveryPolicyName("없는 정책");
//
//        assertThat(result).isEmpty();
//    }
//}