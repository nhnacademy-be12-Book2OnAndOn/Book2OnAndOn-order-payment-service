package com.nhnacademy.Book2OnAndOn_order_payment_service.order.deliveryPolicy.repository;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeliveryPolicyRepositoryTest {

    @Autowired
    private DeliveryPolicyRepository deliveryPolicyRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("배송 정책 저장 및 조회 성공")
    void saveAndFind_success() {
        // Given
        DeliveryPolicy policy = new DeliveryPolicy("기본 배송", 3000, 50000);
        
        // When
        DeliveryPolicy savedPolicy = deliveryPolicyRepository.save(policy);

        // Then
        assertThat(savedPolicy.getDeliveryPolicyId()).isNotNull();
        assertThat(savedPolicy.getDeliveryPolicyName()).isEqualTo("기본 배송");
        assertThat(savedPolicy.getDeliveryFee()).isEqualTo(3000);
        assertThat(savedPolicy.getFreeDeliveryThreshold()).isEqualTo(50000);
    }

    @Test
    @DisplayName("배송 정책 페이징 조회 성공")
    void findAll_paging_success() {
        // Given
        DeliveryPolicy policy1 = new DeliveryPolicy("정책1", 2500, 30000);
        DeliveryPolicy policy2 = new DeliveryPolicy("정책2", 3000, 50000);
        
        deliveryPolicyRepository.saveAll(List.of(policy1, policy2));

        PageRequest pageRequest = PageRequest.of(0, 10);

        // When
        Page<DeliveryPolicy> result = deliveryPolicyRepository.findAll(pageRequest);

        // Then
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(result.getContent()).extracting("deliveryPolicyName")
                .contains("정책1", "정책2");
    }
}