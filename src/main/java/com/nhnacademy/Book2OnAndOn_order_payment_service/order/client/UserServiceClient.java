package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.CurrentPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UserAddressResponseDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    // 배송지 정보 리스트 조회
    @GetMapping("/users/me/addresses")
    List<UserAddressResponseDto> getUserAddresses(@RequestHeader("X-USER-ID") Long userId);

    // 유저 포인트 조회
    @GetMapping("/users/me/points/current")
    CurrentPointResponseDto getUserPoint(@RequestHeader("X-USER-ID") Long userId);
}
