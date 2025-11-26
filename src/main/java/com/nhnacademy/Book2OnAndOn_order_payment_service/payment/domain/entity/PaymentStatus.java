package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    SUCCESS(0, "결제 성공"),
    FAILURE(-1, "결제 실패"),
    WAITING_FOR_DEPOSIT(1, "입금 대기"),
    CANCEL(2, "결제 취소"),
    PARTIAL_CANCEL(3, "부분 취소");

    private final int code;
    private final String description;

    PaymentStatus(int code, String description){
        this.code = code;
        this.description = description;
    }

    // 엔티티, db 변환 용도
    public static PaymentStatus getEnum(int code){
        for (PaymentStatus value : PaymentStatus.values()) {
            if(value.getCode() == code){
                return value;
            }
        }
        throw new EnumConstantNotPresentException(PaymentStatus.class, "code=" + code);
    }

    // 외부값 정규화 (api 호출시 다양하게 주는 값을 정규화함)
    public static PaymentStatus fromExternal(String raw){
        if(raw == null){
            throw new IllegalArgumentException("Payment Status is NULL");
        }

        String normalized = raw.trim().toUpperCase();

        return switch(normalized){
            case "DONE" -> SUCCESS;
            case "CANCELED" -> CANCEL;
            case "ABORTED", "EXPIRED" -> FAILURE;
            case "PARTIAL_CANCELED" -> PARTIAL_CANCEL;
            case "WAITING_FOR_DEPOSIT" -> WAITING_FOR_DEPOSIT;
            default -> throw new IllegalArgumentException("Unknown Payment Status received: '" + raw +
                    "'\nSupported Values: DONE, CANCELED, ABORTED, EXPIRED, PARTIAL_CANCELED");
        };
    }

}
