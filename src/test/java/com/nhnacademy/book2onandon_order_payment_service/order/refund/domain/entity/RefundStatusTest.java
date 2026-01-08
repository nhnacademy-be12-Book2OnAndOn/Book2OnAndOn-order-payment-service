package com.nhnacademy.book2onandon_order_payment_service.order.refund.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundStatusTest {

    @Test
    @DisplayName("getCode: 각 RefundStatus의 code를 반환한다")
    void getCode_success() {
        assertThat(RefundStatus.REQUESTED.getCode()).isZero();
        assertThat(RefundStatus.APPROVED.getCode()).isEqualTo(5);
        assertThat(RefundStatus.REJECTED.getCode()).isEqualTo(99);
    }

    @Test
    @DisplayName("getDescription: 각 RefundStatus의 설명을 반환한다")
    void getDescription_success() {
        assertThat(RefundStatus.REQUESTED.getDescription()).isEqualTo("반품 신청");
        assertThat(RefundStatus.IN_INSPECTION.getDescription()).isEqualTo("검수 중");
        assertThat(RefundStatus.REFUND_COMPLETED.getDescription()).isEqualTo("환불 완료");
    }

    @Test
    @DisplayName("fromCode: 유효한 code면 RefundStatus를 반환한다")
    void fromCode_success() {
        assertThat(RefundStatus.fromCode(0)).isEqualTo(RefundStatus.REQUESTED);
        assertThat(RefundStatus.fromCode(5)).isEqualTo(RefundStatus.APPROVED);
        assertThat(RefundStatus.fromCode(99)).isEqualTo(RefundStatus.REJECTED);
    }

    @Test
    @DisplayName("fromCode: 잘못된 code면 IllegalArgumentException")
    void fromCode_invalid_throw() {
        assertThatThrownBy(() -> RefundStatus.fromCode(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ReturnStatus code");
    }
}
