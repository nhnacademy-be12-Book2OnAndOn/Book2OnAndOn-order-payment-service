package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order;


public enum OrderStatus {
    PENDING(0, "대기"),
    PREPARING(1, "배송 준비중"),
    SHIPPING(2, "배송중"),
    DELIVERED(3, "배송 완료"),
    CANCELED(4, "주문 취소"),
    COMPLETED(5, "주문 완료"),
    PARTIAL_RETURN(6, "부분 반품"),
    RETURN_COMPLETED(7, "반품 완료"),
    RETURN_REQUESTED(8, "반품 신청");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus code: " + code);
    }
}