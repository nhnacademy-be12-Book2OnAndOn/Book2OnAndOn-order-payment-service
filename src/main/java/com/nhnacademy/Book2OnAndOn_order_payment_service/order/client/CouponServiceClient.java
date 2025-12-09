package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "coupon-service")
public interface CouponServiceClient {
}
