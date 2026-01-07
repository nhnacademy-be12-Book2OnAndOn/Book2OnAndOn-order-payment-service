package com.nhnacademy.book2onandon_order_payment_service.order.controller;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.delivery.DeliveryPolicyResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.service.DeliveryPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
class DeliveryPolicyGetController {

    private final DeliveryPolicyService deliveryPolicyService;

    @GetMapping("/delivery-policies")
    public ResponseEntity<Page<DeliveryPolicyResponseDto>> getDeliveryPolicies(Pageable pageable){
        log.debug("GET /delivery-policies 배송비 정책 호출");
        return ResponseEntity.ok(deliveryPolicyService.getPolicies(pageable));
    }
}
