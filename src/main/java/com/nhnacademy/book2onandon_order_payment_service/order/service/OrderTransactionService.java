package com.nhnacademy.book2onandon_order_payment_service.order.service;

import com.nhnacademy.book2onandon_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.book2onandon_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.book2onandon_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.Order;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.book2onandon_order_payment_service.order.provider.GuestTokenProvider;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.book2onandon_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.book2onandon_order_payment_service.payment.domain.dto.CommonConfirmRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTransactionService {

    private final OrderRepository orderRepository;
    private final OrderViewAssembler orderViewAssembler;
    private final GuestTokenProvider guestTokenProvider;

    private final OrderItemRepository orderItemRepository;

    // 해당 유저가 주문을 했는지 판별 로직
    @Transactional(readOnly = true)
    public void validateOrderExistence(Order order, Long userId, String guestToken){

        //회원
        if(userId != null) {
            if (!userId.equals(order.getUserId())) {
                throw new AccessDeniedException("본인의 주문 내역만 조회할 수 있습니다.");
            }
            return;
        }

        // 비회원
        if (guestToken != null) {
            // 토큰 자체의 유효성 검증 & 토큰 안에 들어있는 orderId 꺼내기
            Long tokenOrderId = guestTokenProvider.validateTokenAndGetOrderId(guestToken);

            // 토큰의 (tokenOrderId)과 현재 조회하려는 주문(order.getOrderId()) 같은지 확인
            if (!tokenOrderId.equals(order.getOrderId())) {
                throw new AccessDeniedException("접근 권한이 없는 주문입니다. (토큰 불일치)");
            }
            return;
        }

        throw new AccessDeniedException("로그인이 필요하거나, 비회원 인증 정보가 누락되었습니다.");
    }

    // payment건
    @Transactional
    public Order validateOrderAmount(CommonConfirmRequest req){
        log.debug("주문 금액 검증 실행");
        Order order = orderRepository.findByOrderNumber(req.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Not Found Order: " + req.orderId()));

        if(!Objects.equals(req.amount(), order.getTotalAmount())){
            log.error("데이터베이스에 저장된 금액과 결제 금액이 일치하지 않습니다 (저장 금액 : {}, 결제 금액 : {})", order.getTotalAmount(), req.amount());
            throw new OrderVerificationException("금액 불일치, 저장 금액 : " + order.getTotalAmount() + ", 결제 금액 : " + req.amount());
        }
        log.debug("주문 금액 일치 (금액 : {})", order.getTotalAmount());

        return order;
    }

    @Transactional
    public OrderCreateResponseDto createPendingOrder(Long userId, OrderVerificationResult result) {
        log.info("주문 임시 데이터 저장 로직 실행");

        Order order = Order.builder()
                .userId(userId)
                .orderNumber(result.orderNumber())
                .orderStatus(OrderStatus.PENDING)
                .orderTitle(result.orderTitle())
                .totalAmount(result.totalAmount())
                .totalDiscountAmount(result.totalDiscountAmount())
                .totalItemAmount(result.totalItemAmount())
                .deliveryFee(result.deliveryFee())
                .wrappingFee(result.wrappingFee())
                .couponDiscount(result.couponDiscount())
                .pointDiscount(result.pointDiscount())
                .wantDeliveryDate(result.wantDeliveryDate())
                .build();

        order.addOrderItem(result.orderItemList());
        order.addDeliveryAddress(result.deliveryAddress());

        Order saved = orderRepository.save(order);

        return orderViewAssembler.toOrderCreateView(saved);
    }

    @Transactional
    public void changeStatusOrder(Order order, boolean flag){
        // 상황에 따라서 flag를 INTEGER ENUM + switch 코드로 변환시킬 수 있음
        // flag == true 결제 성공
        // flag == false 결제 취소
        if(flag){
            log.info("결제 성공 후 주문 상태 변경 로직 실행");
            order.updateStatus(OrderStatus.COMPLETED);

            for (OrderItem orderItem : order.getOrderItems()) {
                orderItem.updateStatus(OrderItemStatus.ORDER_COMPLETE);
            }

//            List<OrderItem> orderItemList =  orderItemRepository.findByOrder_OrderId(order.getOrderId());
//
//            for (OrderItem orderItem : orderItemList) {
//                orderItem.updateStatus(OrderItemStatus.ORDER_COMPLETE);
//            }
        }else{
            log.info("결제 취소 후 주문 상태 변경 로직 실행");
            order.updateStatus(OrderStatus.CANCELED);

            for (OrderItem orderItem : order.getOrderItems()) {
                orderItem.updateStatus(OrderItemStatus.ORDER_CANCELED);
            }
        }

    }

    @Transactional(readOnly = true)
    public Order getOrderEntity(String orderNumber){
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Not Found Order : " + orderNumber));
    }
}