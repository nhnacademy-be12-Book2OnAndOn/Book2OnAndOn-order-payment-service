package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto;

public record UserAddressResponseDto(Long addressId,
                                     String userAddressName,
                                     String recipient,
                                     String phone,
                                     String zipCode,
                                     String userAddress,
                                     String userAddressDetail,
                                     boolean isDefault) {
}
