package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestLoginRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestLoginResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.GuestOrder;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.GuestOrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.GuestTokenProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.GuestOrderService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class GuestOrderServiceTest {

    @Mock
    private GuestOrderRepository guestOrderRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GuestTokenProvider tokenProvider;

    @InjectMocks
    private GuestOrderService guestOrderService;

    @Test
    @DisplayName("올바른 주문번호와 비밀번호를 입력하면 게스트 토큰을 발급한다")
    void loginGuest_Success() {
        GuestLoginRequestDto request = new GuestLoginRequestDto("ORD-100", "password123");
        GuestOrder guestOrder = mock(GuestOrder.class);
        Order order = mock(Order.class);

        given(guestOrderRepository.findByOrder_OrderNumber(request.getOrderNumber()))
                .willReturn(Optional.of(guestOrder));
        given(guestOrder.getGuestPassword()).willReturn("encoded-password");
        given(passwordEncoder.matches(request.getGuestPassword(), "encoded-password"))
                .willReturn(true);

        // [수정] Order 객체 연결 및 ID 반환 설정 추가
        given(guestOrder.getOrder()).willReturn(order);
        given(order.getOrderId()).willReturn(1L); // 이 부분이 빠져서 0L이 넘어갔습니다.
        given(order.getOrderNumber()).willReturn("ORD-100");

        given(tokenProvider.createToken(1L)).willReturn("guest-jwt-token");

        GuestLoginResponseDto result = guestOrderService.loginGuest(request);

        assertThat(result.getAccessToken()).isEqualTo("guest-jwt-token");
        assertThat(result.getOrderNumber()).isEqualTo("ORD-100");
    }

    @Test
    @DisplayName("존재하지 않는 주문번호로 로그인 시도 시 GuestOrderNotFoundException이 발생한다")
    void loginGuest_NotFound() {
        GuestLoginRequestDto request = new GuestLoginRequestDto("INVALID-ORD", "any-password");

        given(guestOrderRepository.findByOrder_OrderNumber(request.getOrderNumber()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> guestOrderService.loginGuest(request))
                .isInstanceOf(GuestOrderNotFoundException.class);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 IllegalArgumentException이 발생한다")
    void loginGuest_PasswordMismatch() {
        GuestLoginRequestDto request = new GuestLoginRequestDto("ORD-100", "wrong-password");
        GuestOrder guestOrder = mock(GuestOrder.class);

        given(guestOrderRepository.findByOrder_OrderNumber(request.getOrderNumber()))
                .willReturn(Optional.of(guestOrder));
        given(guestOrder.getGuestPassword()).willReturn("encoded-password");
        given(passwordEncoder.matches(request.getGuestPassword(), "encoded-password"))
                .willReturn(false);

        assertThatThrownBy(() -> guestOrderService.loginGuest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }
}