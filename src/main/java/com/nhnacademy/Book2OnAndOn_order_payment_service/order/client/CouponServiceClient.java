package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UserCouponRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UserCouponResponseDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "coupon-service")
public interface CouponServiceClient {
    @GetMapping()
    List<UserCouponResponseDto> getUsableCoupons(@RequestHeader("X-User-Id") Long userId,
                                                 @RequestBody List<UserCouponRequestDto> userCouponRequestDtoList);
}
