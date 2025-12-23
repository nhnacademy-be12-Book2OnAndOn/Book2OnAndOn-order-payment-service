package com.nhnacademy.Book2OnAndOn_order_payment_service.order.controller;


import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestLoginRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestLoginResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.GuestOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders/guest")
public class GuestOrderController {

    private final GuestOrderService guestOrderService;

    public ResponseEntity<GuestLoginResponseDto> loginGuest(@RequestBody GuestLoginRequestDto requestDto) {

        GuestLoginResponseDto responseDto = guestOrderService.loginGuest(requestDto);

        return ResponseEntity.ok(responseDto);
    }
}
