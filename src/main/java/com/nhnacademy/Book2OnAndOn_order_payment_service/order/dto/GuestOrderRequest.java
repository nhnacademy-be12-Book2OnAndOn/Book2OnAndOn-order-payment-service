package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuestOrderRequest {
    private String guestName;
    private String guestPhoneNumber;
    private String guestPassword;
}