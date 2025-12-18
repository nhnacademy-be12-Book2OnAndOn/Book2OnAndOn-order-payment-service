package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.ReserveBookRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.ReserveCouponRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.ReservePointRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderResourceReservationManager {

    private final BookServiceClient bookServiceClient;
    private final CouponServiceClient couponServiceClient;
    private final UserServiceClient userServiceClient;

    public void reserve(Long userId, OrderCreateRequestDto req, OrderVerificationResult result){
        List<BookInfoDto> bookInfoDtoList = req.getOrderItems().stream()
                .map(item -> new BookInfoDto(item.getBookId(), item.getQuantity()))
                .toList();



        reserveBook(result.orderNumber(), bookInfoDtoList);
        reserveCoupon(result.orderNumber(), req.getMemberCouponId());
        reservePoint(result.orderNumber(), userId, req.getPoint());
    }

    public void release(String orderNumber){
        releaseBook(orderNumber);
        releaseCoupon(orderNumber);
        releasePoint(orderNumber);
    }

    public void confirm(String orderNumber){
        confirmBook(orderNumber);
        confirmCoupon(orderNumber);
        confirmPoint(orderNumber);
    }

    // 선점
    private void reserveBook(String orderNumber, List<BookInfoDto> bookInfoDtoList){
        new ReserveBookRequestDto(orderNumber, bookInfoDtoList);

    }

    private void reserveCoupon(String orderNumber, Long memberCouponId){
        new ReserveCouponRequestDto(orderNumber, memberCouponId);
    }

    private void reservePoint(String orderNumber, Long userId, Integer point){
        new ReservePointRequestDto(orderNumber, userId, point);
    }

    // 선점 취소
    private void releaseBook(String orderNumber){

    }

    private void releaseCoupon(String orderNumber){

    }

    private void releasePoint(String orderNumber){

    }

    // 확정
    private void confirmBook(String orderNumber){

    }

    private void confirmCoupon(String orderNumber){

    }

    private void confirmPoint(String orderNumber){

    }
}
