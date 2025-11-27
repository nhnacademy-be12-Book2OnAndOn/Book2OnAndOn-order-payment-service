package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * [반품 상태 요청 DTO] 관리자 반품 상태 변경 시 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnStatusUpdateDto {
    /** 새로운 반품 상태 코드 */
    private int statusCode;
}