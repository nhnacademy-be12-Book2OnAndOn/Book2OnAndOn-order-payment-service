package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest;

import jakarta.validation.constraints.NegativeOrZero.List;
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
