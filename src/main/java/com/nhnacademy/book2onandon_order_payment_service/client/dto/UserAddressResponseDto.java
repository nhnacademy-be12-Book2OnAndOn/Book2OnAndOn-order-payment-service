package com.nhnacademy.book2onandon_order_payment_service.client.dto;

public record UserAddressResponseDto(Long addressId,
                                     String userAddressName,
                                     String recipient,
                                     String phone,
                                     String zipCode,
                                     String userAddress,
                                     String userAddressDetail,
                                     boolean isDefault) {
}
