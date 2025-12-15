package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.CouponTargetResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.MemberCouponResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.OrderCouponCheckRequestDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "coupon-service")
public interface CouponServiceClient {

    @PostMapping("/my-coupon/usable")
    List<MemberCouponResponseDto> getUsableCoupons(@RequestHeader("X-User-Id") Long userId,
                                                   @RequestBody OrderCouponCheckRequestDto requestDto);

    @GetMapping("/member-coupon/{memberCouponId}/targets")
    CouponTargetResponseDto getCouponTargets(@PathVariable("memberCouponId") Long memberCouponId);
}
