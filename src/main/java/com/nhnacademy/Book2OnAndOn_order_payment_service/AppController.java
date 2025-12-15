package com.nhnacademy.Book2OnAndOn_order_payment_service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.generator.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AppController {

    private final OrderNumberGenerator generator;
    @GetMapping("/hello")
    public ResponseEntity<String> hello(){

        String orderNumber = generator.generate();

        return ResponseEntity.ok(orderNumber);
    }
}