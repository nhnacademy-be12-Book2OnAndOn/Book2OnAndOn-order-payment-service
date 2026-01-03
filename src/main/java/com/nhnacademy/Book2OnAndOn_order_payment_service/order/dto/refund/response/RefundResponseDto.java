package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.response;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.Refund;
import java.util.Map;
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
public class RefundResponseDto {
    private Long refundId;
    private Long orderId;
    private String refundReason;
    private String refundReasonDetail; // 반품 사유 상세 내용
    private String refundStatus; // RefundStatus Enum의 설명 필드
    private LocalDateTime refundDatetime;

    // 반품 항목 리스트
    private List<RefundItemResponseDto> refundItems;

    public static RefundResponseDto from(Refund refund, Map<Long, String> titleMap) {
        // 아이템 리스트 변환 로직
        List<RefundItemResponseDto> itemDtos = refund.getRefundItems().stream()
                .map(ri -> {
                    Long bookId = ri.getOrderItem().getBookId();
                    String title = titleMap.getOrDefault(bookId, "");
                    return RefundItemResponseDto.from(ri, title); // 아이템 DTO의 메서드 활용
                })
                .toList();

        // DTO 생성
        return new RefundResponseDto(
                refund.getRefundId(),
                refund.getOrder() != null ? refund.getOrder().getOrderId() : null,
                refund.getRefundReason() != null ? refund.getRefundReason().name() : null,
                refund.getRefundReasonDetail(),
                refund.getRefundStatus() != null ? refund.getRefundStatus().getDescription() : null,
                refund.getRefundCreatedAt(),
                itemDtos
        );
    }
}