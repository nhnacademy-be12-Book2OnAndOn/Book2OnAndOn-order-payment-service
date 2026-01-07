package com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PaymentMethodEnumTest {

    // =========================
    // 1. 영문 값 변환
    // =========================

    @Test
    @DisplayName("영문 결제 수단 변환 성공")
    void fromExternal_english_success() {
        assertThat(PaymentMethod.fromExternal("CARD"))
                .isEqualTo(PaymentMethod.CARD);

        assertThat(PaymentMethod.fromExternal("VISUAL_ACCOUNT"))
                .isEqualTo(PaymentMethod.VISUAL_ACCOUNT);

        assertThat(PaymentMethod.fromExternal("EASY_PAY"))
                .isEqualTo(PaymentMethod.EASY_PAY);

        assertThat(PaymentMethod.fromExternal("MOBILE_PHONE"))
                .isEqualTo(PaymentMethod.MOBILE_PHONE);

        assertThat(PaymentMethod.fromExternal("CULTURE_GIFT_CERTIFICATE"))
                .isEqualTo(PaymentMethod.CULTURE_GIFT_CERTIFICATE);
    }


    // =========================
    // 2. 한글 값 변환
    // =========================

    @Test
    @DisplayName("한글 결제 수단 변환 성공")
    void fromExternal_korean_success() {
        assertThat(PaymentMethod.fromExternal("카드"))
                .isEqualTo(PaymentMethod.CARD);

        assertThat(PaymentMethod.fromExternal("가상계좌"))
                .isEqualTo(PaymentMethod.VISUAL_ACCOUNT);

        assertThat(PaymentMethod.fromExternal("간편결제"))
                .isEqualTo(PaymentMethod.EASY_PAY);

        assertThat(PaymentMethod.fromExternal("휴대폰"))
                .isEqualTo(PaymentMethod.MOBILE_PHONE);

        assertThat(PaymentMethod.fromExternal("문화상품권"))
                .isEqualTo(PaymentMethod.CULTURE_GIFT_CERTIFICATE);
    }


    // =========================
    // 3. 공백 / 대소문자 보정
    // =========================

    @Test
    @DisplayName("공백 및 대소문자가 섞여 있어도 정상적으로 결제 수단 변환")
    void fromExternal_normalize_success() {
        assertThat(PaymentMethod.fromExternal(" card "))
                .isEqualTo(PaymentMethod.CARD);

        assertThat(PaymentMethod.fromExternal("   Mobile_Phone   "))
                .isEqualTo(PaymentMethod.MOBILE_PHONE);

        assertThat(PaymentMethod.fromExternal("EaSy_PaY"))
                .isEqualTo(PaymentMethod.EASY_PAY);
    }


    // =========================
    // 4. null 입력 예외
    // =========================

    @Test
    @DisplayName("null 입력 시 IllegalArgumentException 발생")
    void fromExternal_null_throwsException() {
        assertThatThrownBy(() -> PaymentMethod.fromExternal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment Method is NULL");
    }

    // =========================
    // 5. 지원하지 않는 값 예외
    // =========================

    @Test
    @DisplayName("지원하지 않는 결제 수단 입력 시 예외 발생")
    void fromExternal_unknown_throwsException() {
        assertThatThrownBy(() -> PaymentMethod.fromExternal("BITCOIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Payment Method");
    }

    // =========================
    // 6. description 값 검증
    // =========================

    @Test
    @DisplayName("결제 수단 설명(description) 값 확인")
    void description_value_test() {
        assertThat(PaymentMethod.CARD.getDescription())
                .isEqualTo("카드");

        assertThat(PaymentMethod.VISUAL_ACCOUNT.getDescription())
                .isEqualTo("가상계좌");

        assertThat(PaymentMethod.EASY_PAY.getDescription())
                .isEqualTo("간편결제");

        assertThat(PaymentMethod.MOBILE_PHONE.getDescription())
                .isEqualTo("휴대폰");

        assertThat(PaymentMethod.CULTURE_GIFT_CERTIFICATE.getDescription())
                .isEqualTo("문화상품권");
    }

}
