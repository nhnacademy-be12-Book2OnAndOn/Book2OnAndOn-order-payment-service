package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * [배송 응답 DTO] 배송 정보 조회 시 사용됩니다.
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
    // TODO: 배송 상태, 시작일, 완료일 등 Delivery 엔티티의 상세 정보 포함
}