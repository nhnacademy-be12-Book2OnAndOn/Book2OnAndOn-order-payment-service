package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UseCouponRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.PointUsedRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.ReserveBookRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.RabbitConfig;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
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
    public void prepareResources(Long userId, Long orderId, OrderCreateRequestDto req, OrderVerificationResult result){
        List<BookInfoDto> bookInfoDtoList = req.getOrderItems().stream()
                .map(item -> new BookInfoDto(item.getBookId(), item.getQuantity()))
                .toList();

        reserveBook(orderId, bookInfoDtoList);
        confirmCoupon(result.orderNumber(), userId, req.getMemberCouponId());
        confirmPoint(result.orderNumber(), userId, result.pointDiscount());
    }

    // 자원 복구
    public void releaseResources(String orderNumber, Long memberCouponId, Long userId, Long orderId, Integer point){
        releaseBook(orderId);
        releaseCoupon(orderNumber, memberCouponId);
        releasePoint(orderNumber, userId, point);
    }

    // 도서 확정 (결제 성공시 이벤트 핸들러용)
    public void finalizeBooks(Long orderId){
        confirmBook(orderId);
    }

    /// 실제 로직
    // 도서
    private void reserveBook(Long orderId, List<BookInfoDto> bookInfoDtoList){
        new ReserveBookRequestDto(orderId, bookInfoDtoList);
    }
    private void releaseBook(Long orderId){

    }
    private void confirmBook(Long orderId){

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
    private void releasePoint(String orderNumber, Long userId, Integer point){
        if(point == null || point <= 0) return;

        new PointUsedRequestDto(orderNumber, userId, point);
    }
    private void confirmPoint(String orderNumber, Long userId, Integer point){
        if(point == null || point <= 0) return;
        new PointUsedRequestDto(orderNumber, userId, point);
    }

}

