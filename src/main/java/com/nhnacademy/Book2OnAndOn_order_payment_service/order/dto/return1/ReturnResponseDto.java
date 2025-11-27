package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.return1;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 반품 신청, 조회, 상태 변경 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponseDto {
    private Long returnId;
    private Long orderId;
    private String returnReason;
    private String returnReasonDetail; // 반품 사유 상세 내용
    private String returnStatus; // ReturnStatus Enum의 설명 필드
    private LocalDateTime returnDatetime;

    // 반품 항목 리스트
    private List<ReturnItemDetailDto> returnItems;
}