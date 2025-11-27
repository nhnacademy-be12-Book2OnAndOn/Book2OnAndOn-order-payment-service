package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * [반품 응답 DTO] 반품 신청, 조회, 상태 변경 시 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponseDto {
    private Long returnId;
    private Long orderId;
    private String returnReason;
    private String returnStatus;
    private LocalDateTime returnDatetime;
    // TODO: 반품 항목 리스트 등 상세 정보 포함
}