package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundItem;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 반품 상세 조회 응답(RefundResponseDto)에 포함되는 개별 반품 항목 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundItemResponseDto {
    private Long refundItemId;
    private Long orderItemId; // 원 주문 항목 ID
    private String bookTitle;
    private int refundQuantity;

    public static RefundItemResponseDto from(RefundItem item, String bookTitle) {
        return new RefundItemResponseDto(
                item.getRefundItemId(),
                item.getOrderItem().getOrderItemId(),
                bookTitle != null ? bookTitle : "", // 제목 null 처리
                item.getRefundQuantity()
        );
    }
}