package com.nhnacademy.book2onandon_order_payment_service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nhnacademy.book2onandon_order_payment_service.client.dto.*;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.refund.request.RefundPointRequestDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceClientTest {

    private final UserServiceClient userServiceClient = mock(UserServiceClient.class);

    @Test
    @DisplayName("유저 ID로 배송지 정보 리스트를 조회한다")
    void getUserAddresses_Call() {
        Long userId = 1L;
        List<UserAddressResponseDto> expected = List.of(mock(UserAddressResponseDto.class));
        given(userServiceClient.getUserAddresses(userId)).willReturn(expected);

        List<UserAddressResponseDto> actual = userServiceClient.getUserAddresses(userId);

        assertThat(actual).hasSize(1);
        verify(userServiceClient).getUserAddresses(userId);
    }

    @Test
    @DisplayName("유저 ID로 현재 보유 포인트를 조회한다")
    void getUserPoint_Call() {
        Long userId = 1L;
        CurrentPointResponseDto expected = mock(CurrentPointResponseDto.class);
        given(userServiceClient.getUserPoint(userId)).willReturn(expected);

        CurrentPointResponseDto actual = userServiceClient.getUserPoint(userId);

        assertThat(actual).isEqualTo(expected);
        verify(userServiceClient).getUserPoint(userId);
    }

    @Test
    @DisplayName("키워드로 유저 ID 리스트를 검색한다")
    void searchUserIdsByKeyword_Call() {
        String keyword = "test";
        List<Long> expected = List.of(1L, 2L);
        given(userServiceClient.searchUserIdsByKeyword(keyword)).willReturn(expected);

        List<Long> actual = userServiceClient.searchUserIdsByKeyword(keyword);

        assertThat(actual).containsExactly(1L, 2L);
        verify(userServiceClient).searchUserIdsByKeyword(keyword);
    }

    @Test
    @DisplayName("포인트 환불을 요청한다(일반)")
    void refundPoint_Call() {
        RefundPointRequestDto dto = mock(RefundPointRequestDto.class);
        doNothing().when(userServiceClient).refundPoint(dto);

        userServiceClient.refundPoint(dto);

        verify(userServiceClient).refundPoint(dto);
    }

    @Test
    @DisplayName("포인트를 사용하고 결과를 반환받는다(내부 통신)")
    void usePoint_Call() {
        Long userId = 1L;
        UsePointInternalRequestDto dto = mock(UsePointInternalRequestDto.class);
        EarnPointResponseDto expected = mock(EarnPointResponseDto.class);
        given(userServiceClient.usePoint(eq(userId), any())).willReturn(expected);

        EarnPointResponseDto actual = userServiceClient.usePoint(userId, dto);

        assertThat(actual).isEqualTo(expected);
        verify(userServiceClient).usePoint(userId, dto);
    }

    @Test
    @DisplayName("주문에 따른 포인트를 적립한다(내부 통신)")
    void earnOrderPoint_Call() {
        Long userId = 1L;
        EarnOrderPointRequestDto dto = mock(EarnOrderPointRequestDto.class);
        EarnPointResponseDto expected = mock(EarnPointResponseDto.class);
        given(userServiceClient.earnOrderPoint(eq(userId), any())).willReturn(expected);

        EarnPointResponseDto actual = userServiceClient.earnOrderPoint(userId, dto);

        assertThat(actual).isEqualTo(expected);
        verify(userServiceClient).earnOrderPoint(userId, dto);
    }

    @Test
    @DisplayName("주문 취소/환불에 따른 포인트를 반환한다(내부 통신)")
    void refundPointInternal_Call() {
        Long userId = 1L;
        RefundPointInternalRequestDto dto = mock(RefundPointInternalRequestDto.class);
        EarnPointResponseDto expected = mock(EarnPointResponseDto.class);
        given(userServiceClient.refundPoint(eq(userId), any())).willReturn(expected);

        EarnPointResponseDto actual = userServiceClient.refundPoint(userId, dto);

        assertThat(actual).isEqualTo(expected);
        verify(userServiceClient).refundPoint(userId, dto);
    }
}