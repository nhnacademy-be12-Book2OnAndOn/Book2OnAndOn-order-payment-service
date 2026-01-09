package com.nhnacademy.book2onandon_order_payment_service.order.entity.order;


import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING(0, "주문 대기"),
    PREPARING(1, "배송 준비중"),
    SHIPPING(2, "배송중"),
    DELIVERED(3, "배송 완료"),
    CANCELED(4, "주문 취소"),
    COMPLETED(5, "주문 완료"),
    PARTIAL_REFUND(6, "부분 반품"),
    RETURN_COMPLETED(7, "반품 완료"),
    RETURN_REQUESTED(8, "반품 신청");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus code: " + code);
    }

    public boolean isCancellable(){
        return this.equals(COMPLETED) || this.equals(DELIVERED);
    }

    public boolean isPaidLike() {
        return switch (this) {
            case PREPARING,
                 SHIPPING,
                 DELIVERED,
                 COMPLETED,
                 PARTIAL_REFUND,
                 RETURN_REQUESTED -> true;
            default -> false;
        };
    }
}