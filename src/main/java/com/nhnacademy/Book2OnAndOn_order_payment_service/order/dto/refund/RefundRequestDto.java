package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 회원 반품 신청 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDto {
    private String refundReason;
    private String refundReasonDetail;

    private List<RefundItemRequestDto> refundItems;

//    public Long orderItemId;
}