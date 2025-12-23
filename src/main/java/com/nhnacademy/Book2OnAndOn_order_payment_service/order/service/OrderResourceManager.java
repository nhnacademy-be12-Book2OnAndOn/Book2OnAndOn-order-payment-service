package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.ReserveBookRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UseCouponRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UsePointInternalRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.OrderCanceledEvent;
import com.nhnacademy.Book2OnAndOn_order_payment_service.config.RabbitConfig;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 결제 성공시에만 사용되는 서비스
 */
@Service
@RequiredArgsConstructor
public class OrderResourceManager {

    private final BookServiceClient bookServiceClient;
    private final CouponServiceClient couponServiceClient;
    private final UserServiceClient userServiceClient;

    private final RabbitTemplate rabbitTemplate;

    // 자원 준비
    public void prepareResources(Long userId, OrderCreateRequestDto req, OrderVerificationResult result, Long orderId){
        List<BookInfoDto> bookInfoDtoList = req.getOrderItems().stream()
                .map(item -> new BookInfoDto(item.getBookId(), item.getQuantity()))
                .toList();

        reserveBook(result.orderNumber(), bookInfoDtoList);
        confirmCoupon(result.orderNumber(), userId, req.getMemberCouponId());
        confirmPoint(orderId, userId, result.pointDiscount());
    }

    // 자원 복구
    public void releaseResources(String orderNumber, Long memberCouponId, Long userId, Integer point, Long orderId){
        releaseBook(orderNumber);
        releaseCoupon(orderNumber, memberCouponId);
        releasePoint(orderId, userId, point);
    }

    // 도서 확정 (결제 성공시 이벤트 핸들러용)
    public void finalizeBooks(String orderNumber){
        confirmBook(orderNumber);
    }

    /// 실제 로직
    // 도서
    private void reserveBook(String orderNumber, List<BookInfoDto> bookInfoDtoList){
        bookServiceClient.reserveStock(new ReserveBookRequestDto(orderNumber, bookInfoDtoList));
    }
    private void releaseBook(String orderNumber){
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY_CANCEL_BOOK,
                orderNumber
        );
    }
    private void confirmBook(String orderNumber){
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY_CONFIRM_BOOK,
                orderNumber
        );
    }

    // 쿠폰
    private void releaseCoupon(String orderNumber, Long memberCouponId){
        if(memberCouponId == null) return;
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY_CANCEL_COUPON,
                orderNumber
        );

    }
    private void confirmCoupon(String orderNumber, Long userId, Long memberCouponId){
        if(memberCouponId == null) return;
        couponServiceClient.useCoupon(memberCouponId, userId, new UseCouponRequestDto(orderNumber));
    }

    // 포인트
    private void releasePoint(Long orderId, Long userId, Integer point){
        if(point == null || point <= 0) return;

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY_CANCEL_POINT,
                new OrderCanceledEvent(userId, orderId, point, LocalDateTime.now())
        );
    }
    private void confirmPoint(Long orderId, Long userId, Integer point){
        if(point == null || point <= 0) return;
        userServiceClient.usePoint(userId, new UsePointInternalRequestDto(orderId, point));

    }

}

