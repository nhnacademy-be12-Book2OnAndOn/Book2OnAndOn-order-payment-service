//package com.nhnacademy.Book2OnAndOn_order_payment_service.payment
//
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.RabbitConfig;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.listener.PaymentCompleteMessageListener;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//public class PaymentCompleteListenerTest {
//
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
//    @Autowired
//    private PaymentCompleteMessageListener listener;
//
//    @Autowired
//    private OrderResourceManager manager;
//
//    @Test
//    void testPaymentSuccessEventFlow() throws InterruptedException {
//        // orderNumber 준비
//        String testOrderNumber = "ORDER123";
//
//        // OrderResourceManager를 Spy로 감싸서 finalizeBooks 호출 확인
//        OrderResourceManager spyManager = Mockito.spy(manager);
//
//        // RabbitTemplate로 이벤트 발행
//        rabbitTemplate.convertAndSend(
//                RabbitConfig.EXCHANGE,
//                RabbitConfig.ROUTING_KEY_COMPLETED,
//                testOrderNumber
//        );
//
//        // 비동기 Listener가 실행될 시간을 잠시 대기
//        Thread.sleep(1000);
//
//        // Listener가 호출해서 finalizeBooks가 실행되었는지 확인
//        Mockito.verify(spyManager).finalizeBooks(testOrderNumber);
//    }
//}
