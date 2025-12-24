package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request;

import jakarta.validation.constraints.NotBlank;
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

    private Long orderId;

    private List<RefundItemRequestDto> refundItems; // orderItemId, refundQuantity

    @NotBlank
    private String refundReason;

    private String refundReasonDetail;

}