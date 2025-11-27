package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List; // 리스트를 위해 import

/** 관리자 배송 등록(출고)시 사용됩니다.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCreateRequestDto {
    private Long orderId;
    private String deliveryCompany;
    private String waybill;

    /** 분할 배송 지원 */
    private List<DeliveryItemRequestDto> deliveryItems;
}