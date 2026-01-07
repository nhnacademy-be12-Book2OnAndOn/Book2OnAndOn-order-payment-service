package com.nhnacademy.book2onandon_order_payment_service.order.dto.order.guest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GuestLoginRequestDto {
    private String orderNumber;
    private String guestPassword;
}
