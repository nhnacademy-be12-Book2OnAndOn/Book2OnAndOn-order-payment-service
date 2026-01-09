package com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RefundSearchCondition {
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Long userId;
    private String userKeyword;
    private String orderNumber;

    // 기본값 true 설정(기존 코드에 defaultValue="true"
    private boolean includeGuest = true;

    public RefundStatus getRefundStatusEnum(){
        return this.status != null ? RefundStatus.fromCode(this.status) : null;
    }
}
