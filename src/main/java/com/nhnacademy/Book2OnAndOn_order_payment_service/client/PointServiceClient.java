package com.nhnacademy.Book2OnAndOn_order_payment_service.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.EarnOrderPointRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.EarnPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.RefundPointInternalRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UsePointRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "point-service")
public interface PointServiceClient {

    @PostMapping("/internal/users/{userId}/points/use")
    EarnPointResponseDto usePoint(
            @PathVariable("userId") Long userId,
            @RequestBody UsePointRequestDto dto
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