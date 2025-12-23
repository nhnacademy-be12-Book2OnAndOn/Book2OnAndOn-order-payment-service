package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 반품 가능한 항목만 반환?
 */
@Getter
@AllArgsConstructor
public class RefundAvailableItemResponseDto {
    private Long orderItemId;
    private Long bookId;
    private String bookTitle;
    private int orderedQuantity;
    private int alreadyReturnedQuantity;
    private int returnableQuantity; // ordered - alreadyReturned
    private boolean activeRefundExists;
    private boolean refundable; // 정책/상태상 가능한지(예: 수량 0이면 false)
}
