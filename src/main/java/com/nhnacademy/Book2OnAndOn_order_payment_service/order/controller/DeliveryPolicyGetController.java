package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.DeliveryPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class DeliveryPolicyGetController {

    private final DeliveryPolicyService deliveryPolicyService;

    @GetMapping("/delivery-policies")
    public ResponseEntity<Page<DeliveryPolicyResponseDto>> getDeliveryPolicies(Pageable pageable){
        return ResponseEntity.ok(deliveryPolicyService.getPolicies(pageable));
    }
}
