package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.CurrentPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UserAddressResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.OrderNumberGenerator;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSheetRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSheetResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.NotFoundOrderException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final BookServiceClient bookServiceClient;
    private final UserServiceClient userServiceClient;


    @Override
    public OrderSheetResponseDto setOrder(Long userId, OrderSheetRequestDto req) {
        List<Long> bookIds = req.bookItems().stream()
                .map(OrderSheetRequestDto.BookInfoDto::bookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = bookServiceClient.getBooksForOrder(bookIds);
        log.info("도서 정보 클라이언트 호출 성공, 조회 도서 수 : {}", bookOrderResponseList.size());
        List<UserAddressResponseDto> userAddressResponseDtoList = userServiceClient.getUserAddresses(userId);
        log.info("사용자 정보 클라이언트 호출 성공, 사용자 배송지 수 : {}",userAddressResponseDtoList.size());
        CurrentPointResponseDto userCurrentPoint = userServiceClient.getUserPoint(userId);
        log.info("사용자 정보 클라이언트 호출 성공, 사용자 현재 포인트 : {}", userCurrentPoint.currentPoint());

        return new OrderSheetResponseDto(
                bookOrderResponseList,
                userAddressResponseDtoList,
                userCurrentPoint
        );
    }

    // 주문 생성 (결제 전)
    @Override
    public OrderResponseDto createOrder(Long userId, OrderCreateRequestDto req) {
        log.info("주문 생성 로직 실행 (유저 아이디 : {})", userId);

        Order order = generateAndSaveOrder(userId);

        fillOrderDetails(order, req);

        return order.toOrderResponseDto();
    }

    // 주문 번호 생성 및 주문 검증을 위한 저장
    public Order generateAndSaveOrder(Long userId) {
        int MAX_RETRY_COUNT = 5;

        for(int i = 1; i <= MAX_RETRY_COUNT; i++){
            try{
                String orderNumber = orderNumberGenerator.generate();
                Order order = Order.builder()
                        .userId(userId)
                        .orderNumber(orderNumber)
                        .orderStatus(OrderStatus.PENDING)
                        .build();

                log.info("주문번호 생성 완료 : {}", orderNumber);

                return orderRepository.save(order);
            }catch (DuplicateKeyException e){
                if(isOrderNumberUniqueViolation(e)){
                    log.warn("주문번호 Unique 제약조건 위반 발생 (재시도 남은 횟수 : {})", MAX_RETRY_COUNT - i);
                    continue;
                }
                throw e;
            }
        }
        log.error("주문 생성 로직 실패 (유저 아이디 : {})", userId);
        throw new RuntimeException("주문 생성 실패 (주문번호 중복 5회)");
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

    // 주문 세부 정보 추가
    @Transactional
    public void fillOrderDetails(Order order, OrderCreateRequestDto req) {

        // 요청 Book Id 추출
        List<Long> bookIds = req.getOrderItems().stream()
                .map(OrderItemRequestDto::getBookId)
                .toList();

        // BookService에 책 정보 조회
        List<BookOrderResponse> bookInfos = bookServiceClient.getBooksForOrder(bookIds);

        // (Key : BookId, Value : BookOrderResponse) 맵 변환
        Map<Long, BookOrderResponse> bookInfoMap = bookInfos.stream()
                .collect(Collectors.toMap(BookOrderResponse::getBookId, b -> b));

        // 요청된 OrderItem 추가 작업
        for (OrderItemRequestDto itemReq : req.getOrderItems()) {
            Long bookId = itemReq.getBookId();
            BookOrderResponse bookInfo = bookInfoMap.get(bookId);

            if(bookInfo == null){
                log.error("책 정보가 없습니다. (책 아이디 : {})", bookId);
                throw new RuntimeException("책 정보가 없습니다. 책 아이디 : " + bookId);
            }

            OrderItem orderItem = OrderItem.builder()
                    .bookId(bookId)
                    .orderItemQuantity(itemReq.getQuantity())
                    .unitPrice(bookInfo.getPriceSales().intValue())
                    .isWrapped(itemReq.isWrapped())
                    .orderItemStatus(OrderItemStatus.PREPARING).build();

            // 양방향 매핑
            order.addOrderItem(orderItem);
        }

        // 요청 배송지 설정 저장
        DeliveryAddress address = DeliveryAddress.builder()
                .deliveryAddress(req.getDeliveryAddress().getDeliveryAddress())
                .deliveryAddressDetail(req.getDeliveryAddress().getDeliveryAddressDetail())
                .deliveryMessage(req.getDeliveryAddress().getDeliveryMessage())
                .recipient(req.getDeliveryAddress().getRecipient())
                .recipientPhonenumber(req.getDeliveryAddress().getRecipientPhonenumber())
                .build();

        order.addDeliveryAddress(address);

        // 주문명 설정
        Long bookId = order.getOrderItems().stream()
                        .map(OrderItem::getBookId)
                        .min(Long::compare)
                        .orElseThrow();

        String rePresentiveTitle = bookInfoMap.get(bookId).getTitle();

        int totalCount = order.getOrderItems().size();

        String orderTitle = "";
        if(totalCount == 1){
            orderTitle = rePresentiveTitle;
        }else{
            orderTitle = rePresentiveTitle + " 외 " + (totalCount - 1) + "권";
        }

        order.setOrderTitle(orderTitle);

        // TODO 쿠폰 적용 및 포인트 적용으로 인한 값 추가


        // TODO 배송 선호 날짜
        order.setWantDeliveryDate(req.getWantDeliveryDate());
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

    // 일반 사용자 주문 취소
    @Override
    public OrderResponseDto cancelOrder(Long userId, String orderNumber, CommonCancelRequest req) {
        log.info("일반 사용자 주문 취소 로직 실행 (유저 아이디 : {}, 주문번호 : {})", userId, orderNumber);

        boolean isExists = orderRepository.existsByOrderNumberAndUserId(orderNumber, userId);

        if(!isExists){
            throw new NotFoundOrderException("잘못된 접근입니다 : " + orderNumber);
        }

//        String provider = pay

        return null;
    }

    /*
        스케줄러 전용
    */
    @Override
    public List<Long> findNextBatch(LocalDateTime thresholdTime, Long lastId, int batchSize) {
        return orderRepository.findNextBatch(OrderStatus.PENDING.getCode(), thresholdTime, lastId, batchSize);
    }

    @Transactional
    @Override
    public int deleteJunkOrder(List<Long> ids) {
        return orderRepository.deleteByIds(ids);
    }


    /*
        API 호출 전용
    */
    @Override
    public Boolean existsPurchase(Long userId, Long bookId) {
        return orderRepository.existsPurchase(userId, bookId);
    }


}
