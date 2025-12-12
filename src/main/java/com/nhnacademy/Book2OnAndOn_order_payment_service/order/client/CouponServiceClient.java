package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.MemberCouponResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.OrderCouponCheckRequestDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "coupon-service")
public interface CouponServiceClient {

    @PostMapping("/my-coupon/usable")
    List<MemberCouponResponseDto> getUsableCoupons(@RequestHeader("X-User-Id") Long userId,
                                                   @RequestBody OrderCouponCheckRequestDto requestDto);
}
