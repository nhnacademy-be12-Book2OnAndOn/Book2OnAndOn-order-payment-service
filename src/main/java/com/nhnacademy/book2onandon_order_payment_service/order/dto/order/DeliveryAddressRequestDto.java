package com.nhnacademy.book2onandon_order_payment_service.order.dto.order;

import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "주소를 입력해주세요")
    private String deliveryAddress;
    private String deliveryAddressDetail;
    private String deliveryMessage;
    @NotNull(message = "수령인을 입력해주세요")
    private String recipient;
    @NotNull(message = "수령인 전화번호를 입력해주세요")
    private String recipientPhoneNumber; // 추가된 필드 (필수 가정)
}