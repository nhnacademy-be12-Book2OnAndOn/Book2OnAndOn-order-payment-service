package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 배송 상세 정보 조회.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponseDto {
    private Long deliveryId;
    private Long orderId;
    private String deliveryCompany;
    private String waybill;
    private String deliveryStatus; // DeliveryStatus Enum의 설명 필드
    private LocalDateTime deliveryStartAt; // Delivery 엔티티 필드 가정
    private LocalDateTime deliveryCompleteAt; // Delivery 엔티티 필드 가정
    // TODO: 배송 항목 리스트 (DeliveryItem)는 제외하고 단순 배송 정보만 제공합니다.
}