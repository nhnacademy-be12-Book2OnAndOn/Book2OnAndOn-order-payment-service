package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.CurrentPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UserAddressResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.UserCouponResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.config.OrderNumberGenerator;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelRequestDto2;
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
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
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
//    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final BookServiceClient bookServiceClient;
    private final UserServiceClient userServiceClient;
    private final CouponServiceClient couponServiceClient;
    private final PaymentStrategyFactory paymentStrategyFactory;

    /**
     * 책 클라이언트를 통해 책 정보를 가져오는 공용 메서드입니다.
     * @param req
     * @return 책 정보 반환 List
     */
    private List<BookOrderResponse> fetchBookInfo(OrderSheetRequestDto req){
        List<Long> bookIds = req.bookItems().stream()
                .map(OrderSheetRequestDto.BookInfoDto::bookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = bookServiceClient.getBooksForOrder(bookIds);
        log.info("도서 정보 클라이언트 호출 성공, 조회 도서 수 : {}", bookOrderResponseList.size());

        return bookOrderResponseList;
    }

    /**
     * 주문 시 필요한 데이터를 미리 불러오는 메서드입니다.
     * @param userId
     * @param req
     * @return 주문 항목, 유저 배송지 정보, 사용가능한 쿠폰, 유저 포인트 반환 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public OrderSheetResponseDto prepareOrder(Long userId, OrderSheetRequestDto req) {
        log.info("주문 전 데이터 정보 가져오기 로직 실행 (유저 아이디 : {})", userId);

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(req);

        List<UserAddressResponseDto> userAddressResponseDtoList = userServiceClient.getUserAddresses(userId);
        log.info("회원 정보 클라이언트 호출 성공, 회원 배송지 수 : {}",userAddressResponseDtoList.size());

        //TODO : bookId, categoryId 받아와서 userCouponRequestDtoList 생성

        List<UserCouponResponseDto> userCouponResponseDtoList = couponServiceClient.getUsableCoupons(userId, null);
        log.info("쿠폰 정보 클라이언트 호출 성공, 사용 가능한 쿠폰 수 : {}", userCouponResponseDtoList.size());
        CurrentPointResponseDto userCurrentPoint = userServiceClient.getUserPoint(userId);
        log.info("회원 정보 클라이언트 호출 성공, 회원 현재 포인트 : {}", userCurrentPoint.currentPoint());

        // 회원 주문은 배송지, 쿠폰 및 포인트 여부도 가져옴
        return new OrderSheetResponseDto(
                bookOrderResponseList,
                userAddressResponseDtoList,
                userCouponResponseDtoList,
                userCurrentPoint
        );
    }

    /**
     * 결제 시작 전에 검증을 위해 검증 데이터들을 저장하는 메서드입니다.
     * @param userId
     * @param req
     * @return 주문 관련 모든 데이터 반환
     */
    @Transactional
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
        // TODO 커스텀 exception 설계
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
        PaymentResponse paymentResponse = paymentService.getPayment(new PaymentRequest(orderNumber));
        orderResponseDto.setPaymentResponse(paymentResponse);

        return orderResponseDto;
    }

    // 일반 사용자 주문 취소
    @Transactional
    @Override
    public OrderResponseDto cancelOrder(Long userId, String orderNumber, OrderCancelRequestDto2 req) {
        log.info("일반 사용자 주문 취소 로직 실행 (유저 아이디 : {}, 주문번호 : {})", userId, orderNumber);

        boolean isExists = orderRepository.existsByOrderNumberAndUserId(orderNumber, userId);

        if(!isExists){
            throw new NotFoundOrderException("잘못된 접근입니다 : " + orderNumber);
        }

        PaymentResponse paymentResponse = paymentService.getPayment(new PaymentRequest(orderNumber));

        CommonCancelRequest cancelReq = new CommonCancelRequest(paymentResponse.paymentKey(), null, req.cancelReason());

        String provider = paymentResponse.paymentProvider();
        PaymentStrategy paymentStrategy = paymentStrategyFactory.getStrategy(provider);
        CommonCancelResponse cancelResponse = paymentStrategy.cancelPayment(cancelReq, orderNumber);

        PaymentCancelCreateRequest createRequest = cancelResponse.toPaymentCancelCreateRequest();
        paymentService.createPaymentCancel(createRequest);



        return null;
    }

    @Override
    public OrderSheetResponseDto prepareGuestOrder(String guestId, OrderSheetRequestDto req) {
        log.info("주문 전 데이터 정보 가져오기 로직 실행 (비회원 유저 : {})", guestId);

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(req);

        return new OrderSheetResponseDto(
                bookOrderResponseList,
                null,
                null,
                null
        );
    }

    /*
        스케줄러 전용
    */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    @Override
    public Boolean existsPurchase(Long userId, Long bookId) {
        return orderRepository.existsPurchase(userId, bookId);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponseDto getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundOrderException("Not Found Order : " + orderNumber));
        return order.toOrderResponseDto();
    }

    @Transactional(readOnly = true)
    @Override
    public Boolean existsOrderByUserIdAndOrderNumber(Long userId, String orderNumber) {
        return orderRepository.existsByOrderNumberAndUserId(orderNumber, userId);
    }


}
