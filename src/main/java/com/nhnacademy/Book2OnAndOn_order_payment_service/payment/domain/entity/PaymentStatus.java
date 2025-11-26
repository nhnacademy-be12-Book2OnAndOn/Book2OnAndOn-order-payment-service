package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

public enum PaymentStatus {
    SUCCESS(0, "결제 성공"),
    FAILURE(-1, "결제 실패");

    private final int code;
    private final String description;

    PaymentStatus(int code, String description){
        this.code = code;
        this.description = description;
    }

    String getDescription(){
        return this.description;
    }
}
