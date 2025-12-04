package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 *  회원 반품 신청 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDto {
    private String returnReason;
    private String returnReasonDetail;

    /** 반품할 주문 항목 리스트 */
    private List<ReturnItemRequestDto> returnItems;
}