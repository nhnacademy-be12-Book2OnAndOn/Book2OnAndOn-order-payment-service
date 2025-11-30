package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    CARD("카드"),
    VISUAL_ACCOUNT("가상계좌"),
    EASY_PAY("간편결제"),
    MOBILE_PHONE("휴대폰"),
    CULTURE_GIFT_CERTIFICATE("문화상품권");

    private final String description;

    PaymentMethod(String description){
        this.description = description;
    }

    public static PaymentMethod fromExternal(String raw){
        if(raw == null){
            throw new IllegalArgumentException("Payment Method is NULL");
        }

        String normalized = raw.trim().toUpperCase();

        return switch (normalized){
            case "CARD", "카드" -> CARD;
            case "VISUAL_ACCOUNT", "가상계좌" -> VISUAL_ACCOUNT;
            case "EASY_PAY", "간편결제" -> EASY_PAY;
            case "MOBILE_PHONE", "휴대폰" -> MOBILE_PHONE;
            case "CULTURE_GIFT_CERTIFICATE", "문화상품권" -> CULTURE_GIFT_CERTIFICATE;
            default -> throw new IllegalArgumentException("Unknown Payment Method received: '" + raw +
                    "'\nSupported Values: CARD, VISUAL_ACCOUNT, MOBILE_PHONE, CULTURE_GIFT_CERTIFICATE");
        };
    }
}
