package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestLoginRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestLoginResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.GuestOrder;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.GuestOrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.GuestTokenProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.GuestOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestOrderService {

    private final GuestOrderRepository guestOrderRepository;
    private final PasswordEncoder passwordEncoder;
    private final GuestTokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public GuestLoginResponseDto loginGuest(GuestLoginRequestDto requestDto) {
        log.debug("orderNumber {} ", requestDto.getOrderNumber());
        GuestOrder guestOrder = guestOrderRepository.findByOrder_OrderNumber(requestDto.getOrderNumber())
                .orElseThrow(GuestOrderNotFoundException::new);

        if(!passwordEncoder.matches(requestDto.getGuestPassword(), guestOrder.getGuestPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        Long orderId = guestOrder.getOrder().getOrderId();

        String token = tokenProvider.createToken(orderId);
        log.debug("Guest Token 발급 성공. orderId = {}", orderId);

        return new GuestLoginResponseDto(token, orderId);
    }
}
