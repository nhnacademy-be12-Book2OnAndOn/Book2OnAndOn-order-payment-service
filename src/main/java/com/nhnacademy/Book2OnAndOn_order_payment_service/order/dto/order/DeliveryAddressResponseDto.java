package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

public record DeliveryAddressResponseDto(String deliveryAddress,
                                         String deliveryAddressDetail,
                                         String deliveryMessage,
                                         String recipient,
                                         String recipientPhoneNumber) {
}
