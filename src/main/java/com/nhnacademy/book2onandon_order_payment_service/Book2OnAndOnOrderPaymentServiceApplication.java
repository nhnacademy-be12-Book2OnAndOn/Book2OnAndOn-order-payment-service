package com.nhnacademy.book2onandon_order_payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class Book2OnAndOnOrderPaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(Book2OnAndOnOrderPaymentServiceApplication.class, args);
	}

}
