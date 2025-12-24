package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "book2.dev.order-payment.exchange";

    // 결제 성공시 도서 재고 차감 확정을 위한 라우팅 키
    public static final String ROUTING_KEY_CONFIRM_BOOK = "book.confirm";

    // 결제 취소시 도서 재고 선점 취소를 위한 라우팅 키
    public static final String ROUTING_KEY_CANCEL_BOOK = "book.cancel";

    // 결제 취소시 쿠폰 상태변경 위한 라우팅 키
    public static final String ROUTING_KEY_CANCEL_COUPON = "coupon.cancel";

    // 결제 취소시 포인트 차감을 위한 라우팅 키
    public static final String ROUTING_KEY_CANCEL_POINT = "point.cancel";

    // 공통 EXCHANGE
//    @Bean
//    public DirectExchange exchange(){
//        return new DirectExchange(EXCHANGE);
//    }

}
