package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order;

public enum OrderItemStatus {
    PREPARING(0, "상품 준비중"),
    SHIPPED(1, "출고 완료"),
    OUT_OF_STOCK_CANCELED(2, "품절 취소"),
    PENDING(3, "주문 대기"),
    ORDER_COMPLETE(4, "주문 완료"),
    ORDER_CANCELED(5, "주문 취소"),
    DELIVERED(6, "배송 완료"),
    USED(7, "사용 완료"),
    RETURN_REQUESTED(8, "반품 요청중"),
    RETURN_COMPLETED(9, "반품 완료");

    private final int code;
    private final String description;

    OrderItemStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }

    public static OrderItemStatus fromCode(int code) {
        for (OrderItemStatus status : OrderItemStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderItemStatus code: " + code);
    }
}