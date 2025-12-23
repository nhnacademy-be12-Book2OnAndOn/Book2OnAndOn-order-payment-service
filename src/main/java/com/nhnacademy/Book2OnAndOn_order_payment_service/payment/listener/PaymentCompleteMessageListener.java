//package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.listener;
//
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.RabbitConfig;
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PaymentCompleteMessageListener {
//    private final OrderResourceManager manager;
//
//    // 결제 성공 이벤트 큐에서 메세지를 가져옴
//
//    @RabbitListener(queues = RabbitConfig.QUEUE_COMPLETED)
//    public void receive(Long orderId){
//        log.info("RabbitMQ -> 결제 성공 이벤트 수신 (결제 성공 핸들러 실행)");
//
//        try{
//            manager.finalizeBooks(orderId);
//        }catch (Exception e){
//            log.error("결제 성공 핸들러 실패 (주문 아이디 : {})", orderId);
//            throw e;
//        }
//    }
//}
