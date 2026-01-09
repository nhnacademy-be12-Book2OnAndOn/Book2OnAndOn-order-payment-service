package com.nhnacademy.book2onandon_order_payment_service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.client.dto.CouponTargetResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.MemberCouponResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.OrderCouponCheckRequestDto;
import com.nhnacademy.book2onandon_order_payment_service.client.dto.UseCouponRequestDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class CouponServiceClientTest {

    private final CouponServiceClient couponServiceClient = mock(CouponServiceClient.class);

    @Test
    @DisplayName("사용 가능한 쿠폰 목록 조회 메서드가 정상적으로 호출되는지 확인한다")
    void getUsableCoupons_Call() {
        Long userId = 1L;
        OrderCouponCheckRequestDto request = mock(OrderCouponCheckRequestDto.class);
        List<MemberCouponResponseDto> expectedResponse = List.of(mock(MemberCouponResponseDto.class));

        given(couponServiceClient.getUsableCoupons(userId, request)).willReturn(expectedResponse);

        List<MemberCouponResponseDto> result = couponServiceClient.getUsableCoupons(userId, request);

        assertThat(result).hasSize(1);
        verify(couponServiceClient).getUsableCoupons(eq(userId), any(OrderCouponCheckRequestDto.class));
    }

    @Test
    @DisplayName("쿠폰 적용 대상 조회 메서드가 정상적으로 호출되는지 확인한다")
    void getCouponTargets_Call() {
        Long memberCouponId = 100L;
        CouponTargetResponseDto expectedResponse = mock(CouponTargetResponseDto.class);

        given(couponServiceClient.getCouponTargets(memberCouponId)).willReturn(expectedResponse);

        CouponTargetResponseDto result = couponServiceClient.getCouponTargets(memberCouponId);

        assertThat(result).isNotNull();
        verify(couponServiceClient).getCouponTargets(memberCouponId);
    }

    @Test
    @DisplayName("쿠폰 사용 확정 메서드가 정상적으로 호출되는지 확인한다")
    void useCoupon_Call() {
        Long memberCouponId = 100L;
        Long userId = 1L;
        UseCouponRequestDto request = mock(UseCouponRequestDto.class);

        given(couponServiceClient.useCoupon(memberCouponId, userId, request))
                .willReturn(ResponseEntity.ok().build());

        ResponseEntity<Void> response = couponServiceClient.useCoupon(memberCouponId, userId, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(couponServiceClient).useCoupon(eq(memberCouponId), eq(userId), any(UseCouponRequestDto.class));
    }
}