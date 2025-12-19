package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UseCouponRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.PointUsedRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.ReserveBookRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderResourceManager {

    private final BookServiceClient bookServiceClient;
    private final CouponServiceClient couponServiceClient;
    private final UserServiceClient userServiceClient;

    // 자원 준비
    public void prepareResources(Long userId, OrderCreateRequestDto req, OrderVerificationResult result){
        List<BookInfoDto> bookInfoDtoList = req.getOrderItems().stream()
                .map(item -> new BookInfoDto(item.getBookId(), item.getQuantity()))
                .toList();

        reserveBook(result.orderNumber(), bookInfoDtoList);
        confirmCoupon(result.orderNumber(), req.getMemberCouponId());
        confirmPoint(result.orderNumber(), userId, result.pointDiscount());
    }

    // 자원 복구
    public void releaseResources(String orderNumber, Long memberCouponId, Long userId, Integer point){
        releaseBook(orderNumber);
        releaseCoupon(orderNumber, memberCouponId);
        releasePoint(orderNumber, userId, point);
    }

    // 도서 확정
    public void finalizeBooks(String orderNumber){
        confirmBook(orderNumber);
    }

    private void reserveBook(String orderNumber, List<BookInfoDto> bookInfoDtoList){
        new ReserveBookRequestDto(orderNumber, bookInfoDtoList);
    }
    private void releaseBook(String orderNumber){

    }
    private void confirmBook(String orderNumber){}


    private void releaseCoupon(String orderNumber, Long memberCouponId){
        if(memberCouponId == null) return;

        new UseCouponRequestDto(orderNumber, memberCouponId);
    }
    private void confirmCoupon(String orderNumber, Long memberCouponId){
        if(memberCouponId == null) return;
        new UseCouponRequestDto(orderNumber, memberCouponId);
    }

    private void releasePoint(String orderNumber, Long userId, Integer point){
        if(point == null || point <= 0) return;

        new PointUsedRequestDto(orderNumber, userId, point);
    }
    private void confirmPoint(String orderNumber, Long userId, Integer point){
        if(point == null || point <= 0) return;
        new PointUsedRequestDto(orderNumber, userId, point);
    }

}

