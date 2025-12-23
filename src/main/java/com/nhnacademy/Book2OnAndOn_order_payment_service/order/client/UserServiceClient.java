package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.CurrentPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UsePointInternalRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UserAddressResponseDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    // 배송지 정보 리스트 조회
    @GetMapping("/users/me/addresses")
    List<UserAddressResponseDto> getUserAddresses(@RequestHeader("X-USER-ID") Long userId);

    // 유저 포인트 조회
    @GetMapping("/users/me/points/current")
    CurrentPointResponseDto getUserPoint(@RequestHeader("X-USER-ID") Long userId);

    // 관리자: 이름/이메일/전화번호 등으로 userId 조회
    @GetMapping("/search")
    List<Long> searchUserIdsByKeyword(@RequestParam("query") String keyword);

    @PostMapping("/internal/users/{userId}/points/use")
    void usePoint(@PathVariable("userId") Long userId, UsePointInternalRequestDto req);
}
