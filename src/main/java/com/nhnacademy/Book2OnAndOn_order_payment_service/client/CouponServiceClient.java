package com.nhnacademy.Book2OnAndOn_order_payment_service.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.CouponTargetResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.MemberCouponResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.OrderCouponCheckRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UseCouponRequestDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "coupon-service")
public interface CouponServiceClient {

    @PostMapping("/my-coupon/usable")
    List<MemberCouponResponseDto> getUsableCoupons(@RequestHeader("X-User-Id") Long userId,
                                                   @RequestBody OrderCouponCheckRequestDto requestDto);

    @GetMapping("/member-coupon/{memberCouponId}/targets")
    CouponTargetResponseDto getCouponTargets(@PathVariable("memberCouponId") Long memberCouponId);

    @PostMapping("/{member-coupon-id}/use")
    ResponseEntity<Void> useCoupon(@PathVariable("member-coupon-id") Long memberCouponId,
                                          @RequestHeader("X-User-Id") Long userId,
                                          @RequestBody UseCouponRequestDto requestDto);
}
