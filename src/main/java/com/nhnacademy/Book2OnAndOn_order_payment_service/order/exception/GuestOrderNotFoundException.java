package com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.NotFoundException;

public class GuestOrderNotFoundException extends NotFoundException {
    public GuestOrderNotFoundException() {
        super("비회원 주문정보를 찾을 수 없습니다.");
    }
    public GuestOrderNotFoundException(String message) {
        super(message);
    }
}