package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.delivery.DeliveryPolicyResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelRequestDto2;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderItemDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSheetRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSheetResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.DeliveryPolicyNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.ExceedUserPointException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.InvalidDeliveryDateException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.NotFoundOrderException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.generator.OrderNumberGenerator;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.OrderNumberProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.CommonCancelResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.NotFoundPaymentException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.repository.PaymentRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.strategy.PaymentStrategyFactory;

import java.awt.print.Book;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final DeliveryPolicyRepository deliveryPolicyRepository;
    private final WrappingPaperService wrappingPaperService;

    private final PaymentService paymentService;
    private final BookServiceClient bookServiceClient;
    private final UserServiceClient userServiceClient;
    private final CouponServiceClient couponServiceClient;
    private final PaymentStrategyFactory paymentStrategyFactory;

    private final OrderNumberProvider orderNumberProvider;

    /**
     * 책 클라이언트를 통해 책 정보를 가져오는 공용 메서드입니다.
     * @param bookIds
     * @return 책 정보 반환 List
     */
    private List<BookOrderResponse> fetchBookInfo(List<Long> bookIds){
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

        List<Long> bookIds = req.bookItems().stream()
                .map(OrderSheetRequestDto.BookInfoDto::bookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);

        List<UserAddressResponseDto> userAddressResponseDtoList = userServiceClient.getUserAddresses(userId);
        log.info("회원 정보 클라이언트 호출 성공, 회원 배송지 수 : {}",userAddressResponseDtoList.size());

        OrderCouponCheckRequestDto orderCouponCheckRequestDto = createOrderCouponCheckRequest(bookOrderResponseList);
        List<MemberCouponResponseDto> userCouponResponseDtoList = couponServiceClient.getUsableCoupons(userId, orderCouponCheckRequestDto);
        log.info("쿠폰 정보 클라이언트 호출 성공, 사용 가능한 쿠폰 수 : {}", userCouponResponseDtoList.size());

        CurrentPointResponseDto userCurrentPoint = userServiceClient.getUserPoint(userId);
        log.info("회원 정보 클라이언트 호출 성공, 회원 현재 포인트 : {}", userCurrentPoint.getCurrentPoint());

        String orderNumber = orderNumberProvider.provideOrderNumber();
        log.info("주문번호 발급 성공 (주문번호 : {})", orderNumber);

        // 회원 주문은 배송지, 쿠폰 및 포인트 여부도 가져옴
        return new OrderSheetResponseDto(
                bookOrderResponseList,
                userAddressResponseDtoList,
                userCouponResponseDtoList,
                userCurrentPoint,
                orderNumber
        );
    }

    /**
     * 사용 가능한 쿠폰을 받기위해 쿠폰 서비스에 요청하는 Dto 생성 로직
     * @param resp
     * @return 책 ID 리스트, 카테고리 ID 리스트가 들어있는 Dto
     */
    private OrderCouponCheckRequestDto createOrderCouponCheckRequest(List<BookOrderResponse> resp){

        List<Long> bookIds = resp.stream()
                .map(BookOrderResponse::getBookId)
                .toList();

        List<Long> categoryIds = resp.stream()
                .map(BookOrderResponse::getCategoryId)
                .toList();

        return new OrderCouponCheckRequestDto(
                bookIds,
                categoryIds
        );
    }

    /**
     * 결제 시작 전에 검증을 위해 검증 데이터들을 저장하는 메서드입니다.
     * 즉 요청값들은 책 ID와 책에 어떤 포장지를 사용하는지에 대한 여부, 배송지 정보, 사용할 쿠폰의 ID, 사용할 포인트를
     * 1차 검증 후 금액을 저장시키고, 결제 후 2차 검증에 필요한 데이터들을 미리 만드는 메서드
     * @param userId
     * @param req
     * @return 주문 관련 모든 데이터 반환
     */
    @Transactional
    @Override
    public OrderResponseDto createOrder(Long userId, OrderCreateRequestDto req) {
        log.info("주문 생성 로직 실행 (유저 아이디 : {})", userId);

        List<OrderItemRequestDto> orderItemRequestDtoList = req.getOrderItems();
        List<Long> bookIds = orderItemRequestDtoList.stream()
                        .map(OrderItemRequestDto::getBookId)
                        .toList();

        List<BookOrderResponse> bookOrderResponseList = bookServiceClient.getBooksForOrder(bookIds);

        List<OrderItem> orderItemList = createOrderItemList(bookOrderResponseList,
                orderItemRequestDtoList);

        DeliveryAddressRequestDto deliveryAddressRequestDto = req.getDeliveryAddress();

        DeliveryAddress deliveryAddress = createDeliveryAddress(deliveryAddressRequestDto);

        String orderTitle = createOrderTitle(bookOrderResponseList);

        int totalItemAmount = orderItemList.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getOrderItemQuantity())
                .sum();

        int deliveryFee = createDeliveryFee(totalItemAmount, req.getDeliveryPolicyId());

        int wrappingFee = createWrappingFee(orderItemList);

        int couponDiscount = createCouponDiscount(userId, req.getCouponId(), bookOrderResponseList, totalItemAmount);

        int pointDiscount = createPointDiscount(userId, req.getPoint());

        int totalDiscountAmount = couponDiscount + pointDiscount;

        int totalAmount = totalItemAmount + deliveryFee + wrappingFee - totalDiscountAmount;

        LocalDate wantDeliveryDate = createWantDeliveryDate(req.getWantDeliveryDate());

        Order order = Order.builder()
                .userId(userId)
                .orderNumber(req.getOrderNumber())
                .orderStatus(OrderStatus.PENDING)
                .orderTitle(orderTitle)
                .totalAmount(totalAmount)
                .totalDiscountAmount(totalDiscountAmount)
                .totalItemAmount(totalItemAmount)
                .deliveryFee(deliveryFee)
                .wrappingFee(wrappingFee)
                .couponDiscount(couponDiscount)
                .pointDiscount(pointDiscount)
                .wantDeliveryDate(wantDeliveryDate)
                .build();

        // 양방향 매핑
        for (OrderItem orderItem : orderItemList) {
            order.addOrderItem(orderItem);
        }

        order.addDeliveryAddress(deliveryAddress);

        Order saved = orderRepository.save(order);

        // 공통 응답 (book client 호출 제외)
        List<OrderItem> orderItemForResponse = saved.getOrderItems();
        List<OrderItemResponseDto> orderItemResponseDtoList = orderItemForResponse.stream()
                .map(item -> new OrderItemResponseDto(
                        item.getOrderItemId(),
                        item.getBookId(),
                        null,
                        null,
                        item.getOrderItemQuantity(),
                        item.getUnitPrice(),
                        item.isWrapped(),
                        null,
                        item.getWrappingPaper().getWrappingPaperId()
                ))
                .toList();

        return saved.toOrderResponseDto(orderItemResponseDtoList);
    }

    private List<OrderItem> createOrderItemList(List<BookOrderResponse> bookOrderResponseList,
                                                List<OrderItemRequestDto> orderItemRequestDtoList){
//        List<OrderItem> orderItemList = new ArrayList<>();
        Map<Long, OrderItem> orderItemMap = new HashMap<>();

        for (BookOrderResponse bookOrderResponse : bookOrderResponseList) {

            // 책 재고 수량 검증
            if(bookOrderResponse.getStockCount() > 1){
                log.error("해당 도서의 재고가 부족합니다. (책 이름 : {}", bookOrderResponse.getTitle());
                throw new OrderVerificationException("해당 도서의 재고가 부족합니다. (책 이름 : " + bookOrderResponse.getTitle() + ")");
            }

            OrderItem orderItem = OrderItem.builder()
                            .bookId(bookOrderResponse.getBookId())
                            .unitPrice(bookOrderResponse.getPriceSales().intValue())
                            .build();

            orderItemMap.put(bookOrderResponse.getBookId(), orderItem);
        }

        for (OrderItemRequestDto orderItemRequestDto : orderItemRequestDtoList) {
            OrderItem orderItem = orderItemMap.get(orderItemRequestDto.getBookId());

            orderItem.setOrderItemQuantity(orderItemRequestDto.getQuantity());
            orderItem.setWrapped(orderItemRequestDto.isWrapped());

            WrappingPaper wrappingPaper = wrappingPaperService.getWrappingPaperEntity(
                    orderItemRequestDto.getWrappingPaperId()
            );

            orderItem.setWrappingPaper(wrappingPaper);

            orderItemMap.put(orderItemRequestDto.getBookId(), orderItem);
        }

//        for (BookOrderResponse bookOrderResponse : bookOrderResponseList) {
//            for (OrderItemRequestDto orderItemRequestDto : orderItemRequestDtoList) {
//                if(Objects.equals(bookOrderResponse.getBookId(), orderItemRequestDto.getBookId())){
//
//                    WrappingPaper wrappingPaper = wrappingPaperService.getWrappingPaperEntity(
//                            orderItemRequestDto.getWrappingPaperId());
//
//                    OrderItem orderItem = OrderItem.builder()
//                            .bookId(bookOrderResponse.getBookId())
//                            .orderItemQuantity(orderItemRequestDto.getQuantity())
//                            .unitPrice(bookOrderResponse.getPriceSales().intValue())
//                            .isWrapped(orderItemRequestDto.isWrapped())
//                            .wrappingPaper(wrappingPaper)
//                            .build();
//
//                    orderItemList.add(orderItem);
//                    break;
//                }
//            }
//        }

        return orderItemMap.values().stream().toList();
//        return orderItemList;
    }

    private DeliveryAddress createDeliveryAddress(DeliveryAddressRequestDto deliveryAddressRequestDto){
        return DeliveryAddress.builder()
                .deliveryAddress(deliveryAddressRequestDto.getDeliveryAddress())
                .deliveryAddressDetail(deliveryAddressRequestDto.getDeliveryAddressDetail())
                .deliveryMessage(deliveryAddressRequestDto.getDeliveryMessage())
                .recipient(deliveryAddressRequestDto.getRecipient())
                .recipientPhoneNumber(deliveryAddressRequestDto.getRecipientPhoneNumber())
                .build();
    }

    private String createOrderTitle(List<BookOrderResponse> bookOrderResponseList){
        StringBuilder sb = new StringBuilder(bookOrderResponseList.getFirst().getTitle());
        int size = bookOrderResponseList.size();

        if(size <= 2){
            sb.append("외 ").append(size).append("권");
        }
        return sb.toString();
    }

    private int createDeliveryFee(int totalItemAmount, Long deliveryPolicyId){
        DeliveryPolicy policy = deliveryPolicyRepository.findById(deliveryPolicyId)
                .orElseThrow(()-> new DeliveryPolicyNotFoundException("Not Found DeliveryPolicy"));
        return policy.calculateDeliveryFee(totalItemAmount);
    }

    private int createWrappingFee(List<OrderItem> orderItemList){
        return orderItemList.stream()
                .filter(OrderItem::isWrapped)
                .map(OrderItem::getWrappingPaper)
                .mapToInt(WrappingPaper::getWrappingPaperPrice)
                .sum();
    }

    // TODO 쿠폰 할인 계산
    private int createCouponDiscount(Long userId, Long couponId, List<BookOrderResponse> bookOrderResponseList, int totalItemAmount){
        CouponTargetResponseDto couponTargetResponseDto = couponServiceClient.getCouponTargets(couponId);
        
        // 할인을 하려면 최소 주문 금액을 넘겨야함
        if(totalItemAmount < couponTargetResponseDto.minPrice()){
            log.error("최조 주문 금액 {}원 이상부터 할인 적용이 가능합니다 (현재 주문 금액 : {}원)", couponTargetResponseDto.minPrice(), totalItemAmount);
            throw new OrderVerificationException("최소 주문 금액 " + couponTargetResponseDto.minPrice() + "원 이상부터 할인 쿠폰 적용이 가능합니다.");
        }

        List<Long> targetBookIds = couponTargetResponseDto.targetBookIds();
        List<Long> targetCategoryIds = couponTargetResponseDto.targetCategoryIds();
        String discountType = couponTargetResponseDto.discountType();

        // 괜찮은 방법 찾기
        if(discountType.equals("FIXED")){
            if(targetBookIds == null){

            }
            if(targetCategoryIds == null){

            }
            if(targetBookIds == null && targetCategoryIds == null){
//                couponTargetResponseDto
            }
        }else{

        }







        return 0;
    }

    private int createPointDiscount(Long userId, Integer point){
        CurrentPointResponseDto currentPointResponseDto = userServiceClient.getUserPoint(userId);
        int currentPoint = currentPointResponseDto.getCurrentPoint();

        if(currentPoint < point){
            throw new ExceedUserPointException("사용 가능한 포인트를 초과했습니다 (현재 포인트 : %d, 요청 포인트 : %d)".formatted(
                    currentPoint,
                    point
            ));
        }
        return point;
    }

    private LocalDate createWantDeliveryDate(LocalDate wantDeliveryDate){
        LocalDate today = LocalDate.now();
        LocalDate minDate = today.plusDays(1);
        LocalDate maxDate = today.plusWeeks(1);

        if(wantDeliveryDate.isBefore(minDate) || wantDeliveryDate.isAfter(maxDate)){
            throw new InvalidDeliveryDateException("지정 배송일은 당일 제외 1주일 후까지 선택 가능합니다 (현재 선택날짜 : " + wantDeliveryDate + ")");
        }

        return wantDeliveryDate;
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

        List<OrderItem> orderItemList = order.getOrderItems();

        List<Long> bookIds = orderItemList.stream()
                .map(OrderItem::getBookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = bookServiceClient.getBooksForOrder(bookIds);

        Map<Long, OrderItemResponseDto> orderItemResponseDtoMap = new HashMap<>();
        for (OrderItem orderItem : orderItemList) {
            OrderItemResponseDto orderItemResponseDto = new OrderItemResponseDto(
                    orderItem.getOrderItemId(),
                    orderItem.getBookId(),
                    null,
                    null,
                    orderItem.getOrderItemQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.isWrapped(),
                    orderItem.getOrderItemStatus().name(),
                    orderItem.getWrappingPaper().getWrappingPaperId()
            );

            orderItemResponseDtoMap.put(orderItem.getOrderItemId(), orderItemResponseDto);
        }

        for (BookOrderResponse bookOrderResponse : bookOrderResponseList) {
            OrderItemResponseDto orderItemResponseDto = orderItemResponseDtoMap.get(bookOrderResponse.getBookId());
            orderItemResponseDto.setBookTitle(bookOrderResponse.getTitle());
            orderItemResponseDto.setBookImagePath(bookOrderResponse.getImageUrl());

            orderItemResponseDtoMap.put(bookOrderResponse.getBookId(), orderItemResponseDto);
        }

        OrderResponseDto orderResponseDto = order.toOrderResponseDto(orderItemResponseDtoMap.values().stream().toList());
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

        Payment payment = paymentRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundPaymentException("Not Found Payment : " + orderNumber));


        CommonCancelRequest cancelReq = new CommonCancelRequest(payment.getPaymentKey(), null, req.cancelReason());

        String provider = payment.getPaymentProvider().name();

        PaymentStrategy paymentStrategy = paymentStrategyFactory.getStrategy(provider);
        CommonCancelResponse cancelResponse = paymentStrategy.cancelPayment(cancelReq, orderNumber);

        PaymentCancelCreateRequest createRequest = cancelResponse.toPaymentCancelCreateRequest();
        paymentService.createPaymentCancel(createRequest);

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundOrderException("Not Found Order : " + orderNumber));

//        order.toOrderResponseDto();

        return null;
    }

    @Override
    public OrderSheetResponseDto prepareGuestOrder(String guestId, OrderSheetRequestDto req) {
        log.info("주문 전 데이터 정보 가져오기 로직 실행 (비회원 유저 : {})", guestId);

        List<Long> bookIds = req.bookItems().stream()
                .map(OrderSheetRequestDto.BookInfoDto::bookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);

        String orderNumber = orderNumberProvider.provideOrderNumber();

        return new OrderSheetResponseDto(
                bookOrderResponseList,
                null,
                null,
                null,
                orderNumber
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
        return order.toOrderResponseDto(null);
    }

    @Transactional(readOnly = true)
    @Override
    public Boolean existsOrderByUserIdAndOrderNumber(Long userId, String orderNumber) {
        return orderRepository.existsByOrderNumberAndUserId(orderNumber, userId);
    }


}
