package com.nhnacademy.Book2OnAndOn_order_payment_service.order.client;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.refund.RefundPointRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "point-service")
public interface PointServiceClient {

    @PostMapping("/refunds")
    void refundPoint(@RequestBody RefundPointRequestDto dto);
}