package com.nhnacademy.Book2OnAndOn_order_payment_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {
    @GetMapping("/hello")
    public String hello(){
        return "Hello World!";
    }
}
