package com.nhnacademy.Book2OnAndOn_order_payment_service.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.CurrentPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.EarnOrderPointRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.EarnPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.RefundPointInternalRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UsePointInternalRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UserAddressResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.request.RefundPointRequestDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    // 배송지 정보 리스트 조회
    @GetMapping("/users/me/addresses")
    List<UserAddressResponseDto> getUserAddresses(@RequestHeader("X-User-Id") Long userId);

    // 유저 포인트 조회
    @GetMapping("/users/me/points/current")
    CurrentPointResponseDto getUserPoint(@RequestHeader("X-User-Id") Long userId);

    // 관리자: 이름/이메일/전화번호 등으로 userId 조회
    @GetMapping("/search")
    List<Long> searchUserIdsByKeyword(@RequestParam("query") String keyword);

    @PostMapping("/users/me/points/refunds")
    void refundPoint(@RequestBody RefundPointRequestDto dto);

    @PostMapping("/internal/users/{userId}/points/use")
    EarnPointResponseDto usePoint(
            @PathVariable("userId") Long userId,
            @RequestBody UsePointInternalRequestDto dto
    );

    @PostMapping("/internal/users/{userId}/points/earn/order")
    EarnPointResponseDto earnOrderPoint(
            @PathVariable("userId") Long userId,
            @RequestBody EarnOrderPointRequestDto dto
    );

    @PostMapping("/internal/users/{userId}/points/refund")
    EarnPointResponseDto refundPoint(
            @PathVariable("userId") Long userId,
            @RequestBody RefundPointInternalRequestDto dto
    );
}
