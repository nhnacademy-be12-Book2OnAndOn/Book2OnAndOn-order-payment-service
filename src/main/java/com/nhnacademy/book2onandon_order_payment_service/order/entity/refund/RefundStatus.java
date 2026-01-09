package com.nhnacademy.book2onandon_order_payment_service.order.entity.refund;

public enum RefundStatus {
    // 프론트에서 이 status를 수정하는 폼이 없다면 자동으로 설정 못하는 값들은 제외해도 무방
    REQUESTED(0, "반품 신청"),
    REQUEST_CANCELED(1, "반품 신청 취소"),
    IN_INSPECTION(4, "검수 중"),
    APPROVED(5, "반품 승인"),
    REFUND_COMPLETED(6, "환불 완료"),
    REJECTED(99, "반품 거부");

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