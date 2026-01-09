package com.nhnacademy.book2onandon_order_payment_service.order.refund.domain.dto;

import static org.assertj.core.api.Assertions.*;

import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundSearchCondition;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundSearchConditionTest {

    @Test
    @DisplayName("기본 생성 시 includeGuest 기본값은 true")
    void default_includeGuest_true() {
        RefundSearchCondition condition = new RefundSearchCondition();
        assertThat(condition.isIncludeGuest()).isTrue();
    }

    @Test
    @DisplayName("status가 null이면 getRefundStatusEnum은 null을 반환한다")
    void getRefundStatusEnum_null_when_status_null() {
        RefundSearchCondition condition = new RefundSearchCondition();
        condition.setStatus(null);

        assertThat(condition.getRefundStatusEnum()).isNull();
    }

    @Test
    @DisplayName("status가 유효한 코드면 getRefundStatusEnum은 해당 enum을 반환한다")
    void getRefundStatusEnum_returns_enum_when_valid_code() {
        RefundSearchCondition condition = new RefundSearchCondition();
        condition.setStatus(RefundStatus.REQUESTED.getCode());

        assertThat(condition.getRefundStatusEnum()).isEqualTo(RefundStatus.REQUESTED);
    }

    @Test
    @DisplayName("status가 유효하지 않으면 getRefundStatusEnum은 fromCode 정책대로 예외를 전파한다")
    void getRefundStatusEnum_throws_when_invalid_code() {
        RefundSearchCondition condition = new RefundSearchCondition();
        condition.setStatus(-999);

        assertThatThrownBy(condition::getRefundStatusEnum)
                .isInstanceOf(RuntimeException.class); // fromCode가 던지는 정확한 예외로 바꾸기
    }
}
