package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.DeliveryPolicyNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryPolicyService {

    private final DeliveryPolicyRepository deliveryPolicyRepository;

    //배송정책 조회
    public Page<DeliveryPolicyResponseDto> getPolicies(Pageable pageable) {

        Page<DeliveryPolicy> policies = deliveryPolicyRepository.findAll(pageable);

        return policies.map(DeliveryPolicyResponseDto::new);
    }

    public DeliveryPolicyResponseDto getPolicy(Long policyId) {

        DeliveryPolicy policy = deliveryPolicyRepository.findById(policyId)
                .orElseThrow(DeliveryPolicyNotFoundException::new);

        return new DeliveryPolicyResponseDto(policy);
    }

    @Transactional
    public void updatePolicy(Long deliveryPolicyId, DeliveryPolicyRequestDto requestDto) {

        DeliveryPolicy policy = deliveryPolicyRepository.findById(deliveryPolicyId)
                .orElseThrow(() -> new DeliveryPolicyNotFoundException(deliveryPolicyId));

        policy.update(requestDto);
        log.info("배송정책 업데이트 성공 deliveryPolicyId: {}", deliveryPolicyId);
    }

    @Transactional
    public void createPolicy(DeliveryPolicyRequestDto requestDto) {
        DeliveryPolicy policy = new DeliveryPolicy(
                requestDto.getDeliveryPolicyName(),
                requestDto.getDeliveryFee(),
                requestDto.getFreeDeliveryThreshold()
        );

        deliveryPolicyRepository.save(policy);
        log.info("배송정책 생성 성공 deliveryPolicyId: {}", policy.getDeliveryPolicyId());
    }

    @Transactional(readOnly = true)
    public DeliveryPolicy getDeliveryPolicy(String deliveryMethod){
        log.info("배송비 정책 가져오기 로직 실행 (배송방법 : {})", deliveryMethod);
        return deliveryPolicyRepository.findFirstByDeliveryPolicyName(deliveryMethod)
                .orElseThrow(DeliveryPolicyNotFoundException::new);
    }
}
