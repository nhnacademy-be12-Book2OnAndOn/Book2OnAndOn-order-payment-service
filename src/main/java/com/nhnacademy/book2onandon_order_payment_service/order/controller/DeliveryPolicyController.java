package com.nhnacademy.book2onandon_order_payment_service.order.controller;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.delivery.DeliveryPolicyRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.delivery.DeliveryPolicyResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.DeliveryPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/delivery-policies")
@RequiredArgsConstructor
public class DeliveryPolicyController {

    private final DeliveryPolicyService deliveryPolicyService;

    //배송 정책 조회
    @GetMapping
    public ResponseEntity<Page<DeliveryPolicyResponseDto>> getDeliveryPolicies(Pageable pageable) {
        Page<DeliveryPolicyResponseDto> policies = deliveryPolicyService.getPolicies(pageable);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{deliveryPolicyId}")
    public ResponseEntity<DeliveryPolicyResponseDto> getDeliveryPolicy(
            @PathVariable Long deliveryPolicyId
    ) {

        return ResponseEntity.ok(deliveryPolicyService.getPolicy(deliveryPolicyId));
    }

    //배송 정책 생성 있어야 하나
    @PostMapping
    public ResponseEntity<Void> createPolicy(
            @Valid @RequestBody DeliveryPolicyRequestDto requestDto) {

        deliveryPolicyService.createPolicy(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //배송 정책 수정
    @PutMapping("/{deliveryPolicyId}")
    public ResponseEntity<Void> updateDeliveryPolicy(
            @PathVariable Long deliveryPolicyId,
            @RequestBody DeliveryPolicyRequestDto requestDto
    ) {
        deliveryPolicyService.updatePolicy(deliveryPolicyId, requestDto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
