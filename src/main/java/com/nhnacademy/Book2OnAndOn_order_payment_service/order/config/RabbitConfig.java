package com.nhnacademy.Book2OnAndOn_order_payment_service.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "book2.dev.order-payment.exchange";

    // 결제 성공시
    public static final String QUEUE_COMPLETED = "book2.dev.completed.queue";

    public static final String ROUTING_KEY_COMPLETED = "order-payment.completed";

//    public static final String QUEUE_FAILED = "book2.dev.failed.queue";
//    public static final String ROUTING_KEY_FAILED = "order-payment.failed";

    // 메시지가 정상적으로 처리되지않으면 여기서 교환
    public static final String DLX_EXCHANGE = "book2.dev.dlx.order-payment.exchange";

    public static final String QUEUE_COMPLETED_DLQ = "book2.dev.completed.dlq";

    public static final String DLX_ROUTING_KEY_COMPLETED = "order-payment.completed.dlq";

//    public static final String QUEUE_FAILED_DLA = "book2.dev.failed.dlq";
//    public static final String DLX_ROUTING_KEY_FAILED = "order-payment.failed.dlq";

    // 결제 취소시 쿠폰 상태변경 위한 라우팅 키
    public static final String ROUTING_KEY_CANCEL_COUPON = "coupon.cancel";

    // 공통 EXCHANGE
    @Bean
    public DirectExchange exchange(){
        return new DirectExchange(EXCHANGE);
    }

    @Bean DirectExchange dlxExchange(){
        return new DirectExchange(DLX_EXCHANGE);
    }

//    @Bean
//    public Exchange

    @Bean
    public Queue completedQueue(){
        return QueueBuilder.durable(QUEUE_COMPLETED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY_COMPLETED)
                .build();
    }

    @Bean
    public Binding completedBinding(){
        return BindingBuilder.bind(completedQueue())
                .to(exchange())
                .with(ROUTING_KEY_COMPLETED);
    }

    @Bean
    public Queue completedDlq(){
        return new Queue(QUEUE_COMPLETED_DLQ, true);
    }

    @Bean
    public Binding completedDlqBinding(){
        return BindingBuilder.bind(completedDlq())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY_COMPLETED);
    }
}
