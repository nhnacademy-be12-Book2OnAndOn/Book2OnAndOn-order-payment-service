package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressInfoRequest {
    private String deliveryAddress;
    private String deliveryAddressDetail;
    private String deliveryMessage;
    private String recipient;
    private String recipientPhoneNumber;
}