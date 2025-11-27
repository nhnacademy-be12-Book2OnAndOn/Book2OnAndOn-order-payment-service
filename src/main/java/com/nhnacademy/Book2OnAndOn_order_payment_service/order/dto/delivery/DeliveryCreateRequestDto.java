package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * [배송 요청 DTO] 관리자 배송 등록 (출고) 시 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCreateRequestDto {
    private Long orderId;
    private String deliveryCompany;
    private String waybill;
    // TODO: 분할 배송을 위해 출고할 OrderItem 리스트를 포함할 수 있습니다.
}