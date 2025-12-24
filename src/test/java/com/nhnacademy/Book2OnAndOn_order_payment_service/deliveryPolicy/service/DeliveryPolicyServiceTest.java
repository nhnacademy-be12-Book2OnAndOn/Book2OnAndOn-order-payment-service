package com.nhnacademy.Book2OnAndOn_order_payment_service.deliveryPolicy.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.DeliveryPolicyNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.DeliveryPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryPolicyServiceTest {

    @InjectMocks
    private DeliveryPolicyService deliveryPolicyService;

    @Mock
    private DeliveryPolicyRepository deliveryPolicyRepository;

    @Test
    @DisplayName("배송 정책 생성 성공")
    void createPolicy_success() {
        // Given
        DeliveryPolicyRequestDto requestDto = new DeliveryPolicyRequestDto("신규 정책", 3000, 50000);
        
        // When
        deliveryPolicyService.createPolicy(requestDto);

        // Then
        verify(deliveryPolicyRepository).save(any(DeliveryPolicy.class));
    }

    @Test
    @DisplayName("배송 정책 단건 조회 성공")
    void getPolicy_success() {
        // Given
        Long policyId = 1L;
        DeliveryPolicy policy = new DeliveryPolicy("기본", 2500, 30000);
        ReflectionTestUtils.setField(policy, "deliveryPolicyId", policyId); // ID 주입

        given(deliveryPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));

        // When
        DeliveryPolicyResponseDto result = deliveryPolicyService.getPolicy(policyId);

        // Then
        assertThat(result.getDeliveryPolicyId()).isEqualTo(policyId);
        assertThat(result.getDeliveryPolicyName()).isEqualTo("기본");
    }

    @Test
    @DisplayName("배송 정책 단건 조회 실패 - 존재하지 않음")
    void getPolicy_notFound() {
        // Given
        Long policyId = 999L;
        given(deliveryPolicyRepository.findById(policyId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> deliveryPolicyService.getPolicy(policyId))
                .isInstanceOf(DeliveryPolicyNotFoundException.class);
    }

    @Test
    @DisplayName("배송 정책 목록 조회 (페이징)")
    void getPolicies_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        DeliveryPolicy policy = new DeliveryPolicy("기본", 2500, 30000);
        Page<DeliveryPolicy> policyPage = new PageImpl<>(List.of(policy));

        given(deliveryPolicyRepository.findAll(pageable)).willReturn(policyPage);

        // When
        Page<DeliveryPolicyResponseDto> result = deliveryPolicyService.getPolicies(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryPolicyName()).isEqualTo("기본");
    }

    @Test
    @DisplayName("배송 정책 수정 성공")
    void updatePolicy_success() {
        // Given
        Long policyId = 1L;
        DeliveryPolicyRequestDto updateDto = new DeliveryPolicyRequestDto("수정된 정책", 4000, 60000);
        
        DeliveryPolicy policy = new DeliveryPolicy("기본", 2500, 30000);
        ReflectionTestUtils.setField(policy, "deliveryPolicyId", policyId);

        given(deliveryPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));

        // When
        deliveryPolicyService.updatePolicy(policyId, updateDto);

        // Then
        assertThat(policy.getDeliveryPolicyName()).isEqualTo("수정된 정책");
        assertThat(policy.getDeliveryFee()).isEqualTo(4000);
    }

    @Test
    @DisplayName("정책 조회 실패 - 존재하지 않는 ID 조회 시 예외 발생")
    void getPolicy_fail_notFound() {
        // Given
        Long invalidId = 999L;
        given(deliveryPolicyRepository.findById(invalidId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> deliveryPolicyService.getPolicy(invalidId))
                .isInstanceOf(DeliveryPolicyNotFoundException.class);
    }

    @Test
    @DisplayName("정책 수정 실패 - 존재하지 않는 ID 수정 시 예외 발생")
    void updatePolicy_fail_notFound() {
        // Given
        Long invalidId = 999L;
        DeliveryPolicyRequestDto requestDto = new DeliveryPolicyRequestDto("수정", 1000, 10000);

        given(deliveryPolicyRepository.findById(invalidId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> deliveryPolicyService.updatePolicy(invalidId, requestDto))
                .isInstanceOf(DeliveryPolicyNotFoundException.class)
                .hasMessageContaining(String.valueOf(invalidId)); // 예외 메시지에 ID가 포함되는지 확인
    }
}
