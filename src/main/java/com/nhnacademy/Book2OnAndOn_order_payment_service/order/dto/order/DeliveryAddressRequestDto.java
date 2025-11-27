package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 주문 시 고객이 입력한 배송지 상세 정보를 담는 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressRequestDto {
    private String deliveryAddress;
    private String deliveryAddressDetail;
    private String deliveryMessage;
    private String recipient;
    private String recipientPhoneNumber; // 추가된 필드 (필수 가정)
}