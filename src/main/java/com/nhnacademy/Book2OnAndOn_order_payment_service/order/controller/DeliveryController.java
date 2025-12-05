package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryWaybillUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/api/deliveries")
    public ResponseEntity<DeliveryResponseDto> getDeliveryByOrder(
            @RequestParam Long orderId,
            @RequestHeader("X-USER-ID") Long userId) { // [추가] 헤더 받기

        // 서비스에 userId도 같이 전달
        DeliveryResponseDto response = deliveryService.getDelivery(orderId, userId);
        return ResponseEntity.ok(response);
    }

    // admin 배송 목록 조회
    // 관리자가 '배송 준비 중인 건만 조회하여 운송장을 입력할 때 사용
    @GetMapping("/admin/deliveries")
    public ResponseEntity<Page<DeliveryResponseDto>> getDeliveries(Pageable pageable,
                                                                   @RequestParam(required = false) OrderStatus status) {
        
        Page<DeliveryResponseDto> page = deliveryService.getDeliveries(pageable, status);
        return ResponseEntity.ok(page);
    }

    // admin 운송장 번호 등록 및 배송 시작 처리
    @PutMapping("/admin/deliveries/{deliveryId}/waybill")
    public ResponseEntity<Void> registerWaybill(
            @PathVariable Long deliveryId,
            @Valid @RequestBody DeliveryWaybillUpdateDto requestDto) {
        
        deliveryService.registerWaybill(deliveryId, requestDto);
        return ResponseEntity.ok().build();
    }

    //admin 택배사, 운송장 번호 수정
    @PutMapping("/admin/deliveries/{deliveryId}/info")
    public ResponseEntity<Void> updateDeliveryInfo(
            @PathVariable Long deliveryId,
            @Valid @RequestBody DeliveryWaybillUpdateDto requestDto) {

        deliveryService.updateDeliveryInfo(deliveryId, requestDto);
        return ResponseEntity.ok().build();
    }

}