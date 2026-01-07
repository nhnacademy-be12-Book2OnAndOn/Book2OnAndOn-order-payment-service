package com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PaymentStatusEnumTest {
    /* ================= getEnum(code) ================= */

    @Test
    @DisplayName("코드값으로 PaymentStatus 조회 성공")
    void getEnum_success() {
        assertThat(PaymentStatus.getEnum(0))
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(PaymentStatus.getEnum(-1))
                .isEqualTo(PaymentStatus.FAILURE);

        assertThat(PaymentStatus.getEnum(1))
                .isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);

        assertThat(PaymentStatus.getEnum(2))
                .isEqualTo(PaymentStatus.CANCEL);

        assertThat(PaymentStatus.getEnum(3))
                .isEqualTo(PaymentStatus.PARTIAL_CANCEL);
    }

    @Test
    @DisplayName("존재하지 않는 코드 조회 시 예외 발생")
    void getEnum_fail_invalid_code() {
        assertThatThrownBy(() -> PaymentStatus.getEnum(99))
                .isInstanceOf(EnumConstantNotPresentException.class);
    }

    /* ================= fromExternal(raw) ================= */

    @Test
    @DisplayName("외부 결제 상태 영문 값 변환 성공")
    void fromExternal_english_success() {
        assertThat(PaymentStatus.fromExternal("DONE"))
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(PaymentStatus.fromExternal("CANCELED"))
                .isEqualTo(PaymentStatus.CANCEL);

        assertThat(PaymentStatus.fromExternal("ABORTED"))
                .isEqualTo(PaymentStatus.FAILURE);

        assertThat(PaymentStatus.fromExternal("EXPIRED"))
                .isEqualTo(PaymentStatus.FAILURE);

        assertThat(PaymentStatus.fromExternal("PARTIAL_CANCELED"))
                .isEqualTo(PaymentStatus.PARTIAL_CANCEL);

        assertThat(PaymentStatus.fromExternal("WAITING_FOR_DEPOSIT"))
                .isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);
    }

    @Test
    @DisplayName("공백 및 대소문자가 섞여 있어도 정상 변환")
    void fromExternal_normalize_success() {
        assertThat(PaymentStatus.fromExternal(" done "))
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(PaymentStatus.fromExternal(" canceled "))
                .isEqualTo(PaymentStatus.CANCEL);

        assertThat(PaymentStatus.fromExternal("  partial_canceled "))
                .isEqualTo(PaymentStatus.PARTIAL_CANCEL);
    }

    @Test
    @DisplayName("null 값 입력 시 예외 발생")
    void fromExternal_null_fail() {
        assertThatThrownBy(() -> PaymentStatus.fromExternal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment Status is NULL");
    }

    @Test
    @DisplayName("지원하지 않는 외부 상태 값 입력 시 예외 발생")
    void fromExternal_unknown_fail() {
        assertThatThrownBy(() -> PaymentStatus.fromExternal("UNKNOWN_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Payment Status");
    }

    /* ================= description / code ================= */

    @Test
    @DisplayName("결제 상태 코드(code) 및 설명(description) 값 확인")
    void description_and_code_test() {
        assertThat(PaymentStatus.SUCCESS.getCode()).isEqualTo(0);
        assertThat(PaymentStatus.SUCCESS.getDescription()).isEqualTo("결제 성공");

        assertThat(PaymentStatus.FAILURE.getCode()).isEqualTo(-1);
        assertThat(PaymentStatus.FAILURE.getDescription()).isEqualTo("결제 실패");

        assertThat(PaymentStatus.WAITING_FOR_DEPOSIT.getCode()).isEqualTo(1);
        assertThat(PaymentStatus.WAITING_FOR_DEPOSIT.getDescription()).isEqualTo("입금 대기");

        assertThat(PaymentStatus.CANCEL.getCode()).isEqualTo(2);
        assertThat(PaymentStatus.CANCEL.getDescription()).isEqualTo("결제 취소");

        assertThat(PaymentStatus.PARTIAL_CANCEL.getCode()).isEqualTo(3);
        assertThat(PaymentStatus.PARTIAL_CANCEL.getDescription()).isEqualTo("부분 취소");
    }

}