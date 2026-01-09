package com.nhnacademy.book2onandon_order_payment_service.cart.domain.dto.request;

import com.nhnacademy.book2onandon_order_payment_service.cart.domain.entity.MergeIssueReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
// merge 시 문제된 항목 정보
public class MergeIssueItemDto {

    private Long bookId;

    private int guestQuantity;
    private int userQuantity;
    private int mergedQuantity;

    private MergeIssueReason reason;
}
