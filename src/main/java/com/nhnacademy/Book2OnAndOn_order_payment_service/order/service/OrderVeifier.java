//package com.nhnacademy.book2onandon_order_payment_service.order.service;
//
//import com.nhnacademy.book2onandon_order_payment_service.client.dto.BookOrderResponse;
//import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderCreateRequestDto;
//import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderVerificationResult;
//import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
//import com.nhnacademy.book2onandon_order_payment_service.order.entity.delivery.DeliveryAddress;
//import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
//import java.time.LocalDate;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class OrderVerifier {
//
//    public OrderVerificationResult verify(Long userId, String guestId, OrderCreateRequestDto req) {
//        log.info("주문 데이터 생성 및 검증 로직 실행 (회원 아이디 : {}, 비회원 아이디 : {})", userId, guestId);
//
//        List<OrderItemRequestDto> orderItemResponseDtoList = req.getOrderItems();
//
//        List<Long> bookIds = orderItemResponseDtoList.stream()
//                .map(OrderItemRequestDto::getBookId)
//                .toList();
//
//        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);
//
//        List<OrderItem> orderItemList = createOrderItemList(bookOrderResponseList, orderItemResponseDtoList);
//
//        DeliveryAddress deliveryAddress = createDeliveryAddress(req.getDeliveryAddress());
//
//        String orderTitle = createOrderTitle(bookOrderResponseList);
//
//        int totalItemAmount = orderItemList.stream()
//                .mapToInt(item -> item.getUnitPrice() * item.getOrderItemQuantity())
//                .sum();
//        int deliveryFee = createDeliveryFee(totalItemAmount, req.getDeliveryPolicyId());
//        int wrappingFee = createWrappingFee(orderItemList);
//        int couponDiscount = createCouponDiscount(req.getMemberCouponId(), orderItemList, bookOrderResponseList, totalItemAmount);
//
//        int currentAmount = totalItemAmount + deliveryFee + wrappingFee - couponDiscount;
//
//        int pointDiscount = createPointDiscount(userId, currentAmount, req.getPoint());
//        int totalDiscountAmount = couponDiscount + pointDiscount;
//        int totalAmount = totalItemAmount + deliveryFee + wrappingFee - totalDiscountAmount;
//
//        if(totalAmount < 100){
//            log.error("최소 결제 금액 100원 이상 결제해야합니다 (현재 주문 금액 : {}원)", totalAmount);
//        }
//
//        LocalDate wantDeliveryDate = createWantDeliveryDate(req.getWantDeliveryDate());
//
//        String orderNumber = orderNumberProvider.provideOrderNumber();
//        log.info("주문 번호 발급 성공 : {}", orderNumber);
//
//        return new OrderVerificationResult(
//                orderNumber,
//                orderTitle,
//                totalAmount,
//                totalDiscountAmount,
//                totalItemAmount,
//                deliveryFee,
//                wrappingFee,
//                couponDiscount,
//                pointDiscount, // 포인트 사용량
//                wantDeliveryDate,
//                orderItemList,
//                deliveryAddress
//        );
//    }
//}