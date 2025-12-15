package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.CurrentPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.MemberCouponResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UserAddressResponseDto;
import java.util.List;

public record OrderPrepareResponseDto(
        // 책 아이템 조회
        List<BookOrderResponse> orderItems,
        // 유저 배송지 조회
        List<UserAddressResponseDto> addresses,
        // 사용할 수 있는 쿠폰 조회
        List<MemberCouponResponseDto> coupons,
        // 유저 포인트 조회
        CurrentPointResponseDto currentPoint
) {
}
