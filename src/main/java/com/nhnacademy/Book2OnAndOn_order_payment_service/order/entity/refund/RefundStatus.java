package com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund;

public enum RefundStatus {
    REQUESTED(0, "반품 신청"),
    COLLECTION_PENDING(1, "수거 대기"),
    IN_TRANSIT(2, "반품 이동중"),
    IN_INSPECTION(3, "반품 검수중"),
    INSPECTION_COMPLETED(4, "검수 완료"),
    REFUND_COMPLETED(5, "환불 완료"),
    REJECTED(99, "반품 거부");

//    public enum OrderItemStatus {
//        ORDERED,            // 주문 생성됨 (결제 전 or 직후)
//        PAID,               // 결제 완료
//        SHIPPING,           // 배송 중
//        DELIVERED,          // 배송 완료
//        RETURN_REQUESTED,   // 반품 신청됨
//        RETURN_IN_PROGRESS, // 수거/검수 중
//        RETURN_COMPLETED,   // 반품/환불 완료
//        RETURN_REJECTED,    // 반품 거부
//        CANCELED            // 결제 취소(배송 전)
//    }

    private final int code;
    private final String description;

    // 생성자
    RefundStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RefundStatus fromCode(int code) {
        for (RefundStatus status : RefundStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ReturnStatus code: " + code);
    }
}