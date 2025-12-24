package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * POST /api/guest/orders
 * 비회원 주문자 정보와 주문 상품 목록을 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuestOrderCreateDto {
    private String orderNumber;
    private String guestName;
    private String guestPhoneNumber;
    private String guestPassword;

}