package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.OrderNumberGenerator;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.NotFoundOrderException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService2 {

    private final OrderNumberGenerator orderNumberGenerator;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    private final int MAX_RETRY_COUNT = 5;

    // 주문 생성
    @Transactional
    @Override
    public OrderSimpleDto createPreOrder(Long userId) {
        log.info("주문 생성 로직 실행 (유저 아이디 : {})", userId);
        for(int i=1; i <= 5; i++){
            try{
                String orderNumber = orderNumberGenerator.generate();
                Order order = Order.builder()
                        .userId(userId)
                        .orderNumber(orderNumber)
                        .orderStatus(OrderStatus.PENDING)
                        .build();

                log.info("주문번호 생성 완료 : {}", orderNumber);

                Order saved = orderRepository.save(order);

                return saved.toOrderSimpleDto();
            }catch (DuplicateKeyException e){
                if(isOrderNumberUniqueViolation(e)){
                    log.warn("주문번호 Unique 제약조건 위반 발생 (재시도 남은 횟수 : {})", MAX_RETRY_COUNT - i);
                    continue;
                }
                throw e;
            }
        }
        log.error("주문 생성 로직 실패 (유저 아이디 : {})", userId);
        throw new RuntimeException("주문 생성 실패");
    }

    // 주문 생성을 위한 유니크 키 검증 메서드
    private boolean isOrderNumberUniqueViolation(Throwable e){
        Throwable cause = e;
        while(cause != null){
            String msg = cause.getMessage();
            if(msg != null){
                msg = msg.toLowerCase();
                if(msg.contains("order_number")
                    && (msg.contains("duplicate") || msg.contains("unique"))){
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    // 일반 사용자 주문 리스트 조회
    @Transactional(readOnly = true)
    @Override
    public Page<OrderSimpleDto> getOrderList(Long userId, Pageable pageable) {
        log.info("일반 사용자 주문 리스트 조회 로직 실행 (유저 아이디 : {})", userId);
        return orderRepository.findAllByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponseDto getOrderDetail(Long userId, String orderNumber) {
        log.info("일반 사용자 주문 상세 정보 조회 로직 실행 (유저 아이디 : {}, 주문번호 : {})", userId, orderNumber);

        Order order = orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)
                .orElseThrow(() -> new NotFoundOrderException("Not Found Order : " + orderNumber));

        OrderResponseDto orderResponseDto = order.toOrderResponseDto();
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundPaymentException("Not Found Payment : " + orderNumber));

        PaymentResponse paymentResponse = payment.toResponse();
        orderResponseDto.setPaymentResponse(paymentResponse);

        return orderResponseDto;
    }

    /**
        API 호출 전용
    */
    @Override
    public Boolean existsPurchase(Long userId, Long bookId) {
        return orderRepository.existsPurchase(userId, bookId);
    }


}
