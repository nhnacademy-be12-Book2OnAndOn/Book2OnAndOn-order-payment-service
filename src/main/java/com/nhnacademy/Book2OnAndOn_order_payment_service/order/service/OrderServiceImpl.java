package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCancelResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemCalcContext;
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
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.OrderNumberProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService2 {
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

    private final OrderViewAssembler orderViewAssembler;
    private final OrderResourceReservationManager reservationManager;

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
     * 책 클라이언트를 통해 책 정보를 가져오는 공용 메서드입니다.
     * @param userId
     * @return 유저 배송지 정보 반환 List
     */
    private List<UserAddressResponseDto> fetchUserAddressInfo(Long userId){
        List<UserAddressResponseDto> userAddressResponseDtoList = userServiceClient.getUserAddresses(userId);
        log.info("회원 정보 클라이언트 호출 성공, 회원 배송지 수 : {}",userAddressResponseDtoList.size());

        return userAddressResponseDtoList;
    }

    /**
     * 책 클라이언트를 통해 책 정보를 가져오는 공용 메서드입니다.
     * @param userId, req
     * @return 사용 가능한 쿠폰 정보 반환 List
     */
    private List<MemberCouponResponseDto> fetchUsableMemberCouponInfo(Long userId, OrderCouponCheckRequestDto req){
        List<MemberCouponResponseDto> memberCouponResponseDtoList = couponServiceClient.getUsableCoupons(userId, req);
        log.info("쿠폰 정보 클라이언트 호출 성공, 사용 가능한 쿠폰 수 : {}", memberCouponResponseDtoList.size());

        return memberCouponResponseDtoList;
    }

    /**
     * 책 클라이언트를 통해 책 정보를 가져오는 공용 메서드입니다.
     * @param userId
     * @return 사용 가능한 포인트 정보 반환
     */
    private CurrentPointResponseDto fetchPointInfo(Long userId){
        CurrentPointResponseDto currentPointResponseDto = userServiceClient.getUserPoint(userId);
        log.info("회원 정보 클라이언트 호출 성공, 회원 현재 포인트 : {}", currentPointResponseDto.getCurrentPoint());

        return currentPointResponseDto;
    }

    /**
     * 주문 시 필요한 데이터를 미리 불러오는 메서드입니다.
     * @param userId, req
     * @return 주문 항목, 유저 배송지 정보, 사용가능한 쿠폰, 유저 포인트 반환 DTO
     */
    // TODO 캐시 설정?
    @Override
    public OrderPrepareResponseDto prepareOrder(Long userId, OrderPrepareRequestDto req) {
        log.info("주문 전 데이터 정보 가져오기 로직 실행 (회원 아이디 : {})", userId);

        // 책 id
        List<Long> bookIds = req.bookItems().stream()
                .map(BookInfoDto::bookId)
                .toList();


        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);
        List<UserAddressResponseDto> userAddressResponseDtoList = fetchUserAddressInfo(userId);

        // 사용 가능한 쿠폰을 받기 위한 RequestDto 생성
        OrderCouponCheckRequestDto orderCouponCheckRequestDto = createOrderCouponCheckRequest(bookOrderResponseList);
        List<MemberCouponResponseDto> userCouponResponseDtoList = fetchUsableMemberCouponInfo(userId, orderCouponCheckRequestDto);

        CurrentPointResponseDto userCurrentPoint = fetchPointInfo(userId);

        // 회원 주문은 배송지, 쿠폰 및 포인트 여부도 가져옴
        return new OrderPrepareResponseDto(
                bookOrderResponseList,
                userAddressResponseDtoList,
                userCouponResponseDtoList,
                userCurrentPoint
        );
    }

    @Override
    public OrderCreateResponseDto createPreOrder(Long userId, OrderCreateRequestDto req) {
        log.info("임시 주문 데이터 생성 및 검증 로직 실행 (회원 아이디 : {})", userId);

        OrderVerificationResult result = verifyOrder(userId, req);

        // 선점 메서드
        reservationManager.reserve(userId, req, result);

        try {
            return createPendingOrder(userId, result);
        } catch (Exception e){
            log.error("알 수 없는 오류 발생! 복구 트랜잭션 실행");
            // 복구 메서드
            reservationManager.release(result.orderNumber());
            throw e;
        }
    }

    // 임시 주문 생성을 위한 헬퍼 메서드
    private OrderVerificationResult verifyOrder(Long userId, OrderCreateRequestDto req) {
        log.info("주문 데이터 생성 및 검증 로직 실행 (회원 아이디 : {})", userId);

        List<OrderItemRequestDto> orderItemResponseDtoList = req.getOrderItems();

        List<Long> bookIds = orderItemResponseDtoList.stream()
                .map(OrderItemRequestDto::getBookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);

        List<OrderItem> orderItemList = createOrderItemList(bookOrderResponseList, orderItemResponseDtoList);

        DeliveryAddress deliveryAddress = createDeliveryAddress(req.getDeliveryAddress());

        String orderTitle = createOrderTitle(bookOrderResponseList);

        int totalItemAmount = orderItemList.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getOrderItemQuantity())
                .sum();
        int deliveryFee = createDeliveryFee(totalItemAmount, req.getDeliveryPolicyId());
        int wrappingFee = createWrappingFee(orderItemList);
        int couponDiscount = createCouponDiscount(req.getMemberCouponId(), orderItemList, bookOrderResponseList, totalItemAmount);

        int currentAmount = totalItemAmount + deliveryFee + wrappingFee - couponDiscount;

        int pointDiscount = createPointDiscount(userId, currentAmount, req.getPoint());
        int totalDiscountAmount = couponDiscount + pointDiscount;
        int totalAmount = totalItemAmount + deliveryFee + wrappingFee - totalDiscountAmount;

        LocalDate wantDeliveryDate = createWantDeliveryDate(req.getWantDeliveryDate());

        String orderNumber = orderNumberProvider.provideOrderNumber();
        log.info("주문 번호 발급 성공 : {}", orderNumber);

        return new OrderVerificationResult(
                orderNumber,
                orderTitle,
                totalAmount,
                totalDiscountAmount,
                totalItemAmount,
                deliveryFee,
                wrappingFee,
                couponDiscount,
                pointDiscount,
                wantDeliveryDate,
                orderItemList,
                deliveryAddress
        );
    }

    @Transactional
    protected OrderCreateResponseDto createPendingOrder(Long userId, OrderVerificationResult result) {
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
     *
     * @param bookOrderResponseList
     * @param orderItemRequestDtoList
     * @return 주문 엔티티 생성용 주문 항목 리스트 생성 로직
     */
    private List<OrderItem> createOrderItemList(List<BookOrderResponse> bookOrderResponseList,
                                                List<OrderItemRequestDto> orderItemRequestDtoList){
        Map<Long, BookOrderResponse> bookMap = bookOrderResponseList.stream()
                .collect(Collectors.toMap(BookOrderResponse::getBookId, Function.identity()));

        return orderItemRequestDtoList.stream()
                .map(dto -> {
                    BookOrderResponse book = bookMap.get(dto.getBookId());
                    if(book == null) throw new OrderVerificationException("책 정보가 일치하지 않습니다 : " + dto.getBookId());
                    WrappingPaper wrappingPaper = dto.isWrapped() ? wrappingPaperService.getWrappingPaperEntity(dto.getWrappingPaperId()) : null;

                    return OrderItem.builder()
                            .bookId(book.getBookId())
                            .orderItemQuantity(dto.getQuantity())
                            .unitPrice(book.getPriceSales().intValue())
                            .isWrapped(dto.isWrapped())
                            .wrappingPaper(wrappingPaper)
                            .build();
                })
                .toList();
    }


    /**
     * 배송지 입력
     * @param deliveryAddressRequestDto
     * @return 배송지 엔티티
     */
    private DeliveryAddress createDeliveryAddress(DeliveryAddressRequestDto deliveryAddressRequestDto){
        return DeliveryAddress.builder()
                .deliveryAddress(deliveryAddressRequestDto.getDeliveryAddress())
                .deliveryAddressDetail(deliveryAddressRequestDto.getDeliveryAddressDetail())
                .deliveryMessage(deliveryAddressRequestDto.getDeliveryMessage())
                .recipient(deliveryAddressRequestDto.getRecipient())
                .recipientPhoneNumber(deliveryAddressRequestDto.getRecipientPhoneNumber())
                .build();
    }

    /**
     * 주문명 생성 메서드
     * @param bookOrderResponseList
     * @return 주문명
     */
    private String createOrderTitle(List<BookOrderResponse> bookOrderResponseList){
        StringBuilder sb = new StringBuilder(bookOrderResponseList.getFirst().getTitle());
        int size = bookOrderResponseList.size();

        if(size <= 2){
            sb.append("외 ").append(size).append("권");
        }
        return sb.toString();
    }

    /**
     * 배송비 검증 및 생성 메서드
     * @param totalItemAmount 총 순수 금액
     * @param deliveryPolicyId 정책 아이디
     * @return 배송비
     */
    private int createDeliveryFee(int totalItemAmount, Long deliveryPolicyId){
        DeliveryPolicy policy = deliveryPolicyRepository.findById(deliveryPolicyId)
                .orElseThrow(()-> new DeliveryPolicyNotFoundException("Not Found DeliveryPolicy"));
        return policy.calculateDeliveryFee(totalItemAmount);
    }

    /**
     * 포장비 검증 및 생성 메서드
     * @param orderItemList
     * @return 포장비
     */
    private int createWrappingFee(List<OrderItem> orderItemList){
        return orderItemList.stream()
                .filter(OrderItem::isWrapped)
                .map(OrderItem::getWrappingPaper)
                .mapToInt(WrappingPaper::getWrappingPaperPrice)
                .sum();
    }


    /**
     * 쿠폰 할인 검증 및 생성 메서드
     * @param couponId
     * @param orderItemList
     * @param bookOrderResponseList
     * @param totalItemAmount
     * @return 쿠폰으로 할인된 금액
     */
    private int createCouponDiscount(Long couponId, List<OrderItem> orderItemList, List<BookOrderResponse> bookOrderResponseList, int totalItemAmount){
        if(couponId == null){
            return 0;
        }
        CouponTargetResponseDto couponTargetResponseDto = couponServiceClient.getCouponTargets(couponId);

        List<Long> targetBookIds = couponTargetResponseDto.targetBookIds();
        List<Long> targetCategoryIds = couponTargetResponseDto.targetCategoryIds();

        Map<Long, BookOrderResponse> bookMap = bookOrderResponseList.stream()
                .collect(Collectors.toMap(BookOrderResponse::getBookId, Function.identity()));

        List<OrderItemCalcContext> calcContextList = orderItemList.stream()
                .map(orderItem -> {
                    BookOrderResponse book = bookMap.get(orderItem.getBookId());
                    if(book == null){
                        throw new OrderVerificationException("도서 정보 불일치 : " + orderItem.getBookId());
                    }
                    return OrderItemCalcContext.of(orderItem, book);
                })
                .toList();

        int discountBaseAmount = calculateDiscountBaseAmount(
                calcContextList,
                targetBookIds,
                targetCategoryIds,
                totalItemAmount);

        if(discountBaseAmount < couponTargetResponseDto.minPrice()){
            log.error("최조 주문 금액 {}원 이상부터 할인 적용이 가능합니다 (현재 주문 금액 : {}원)", couponTargetResponseDto.minPrice(), discountBaseAmount);
            throw new OrderVerificationException("최소 주문 금액 " + couponTargetResponseDto.minPrice() + "원 이상부터 할인 쿠폰 적용이 가능합니다.");
        }

        CouponPolicyDiscountType discountType = couponTargetResponseDto.discountType();

        // 할인금액이 고정
        if(CouponPolicyDiscountType.FIXED.equals(discountType)){
            return couponTargetResponseDto.discountValue();
        }

        int discount = discountBaseAmount * couponTargetResponseDto.discountValue() / 100;
        // 최대 할인 보다 높을시 최대 할인 금액 적용
        return discount > couponTargetResponseDto.maxPrice() ? couponTargetResponseDto.maxPrice() : discount;
    }

    // 할인 대상 금액 계산
    private int calculateDiscountBaseAmount(List<OrderItemCalcContext> calcContextList,
                                            List<Long> targetBookIds,
                                            List<Long> targetCategoryIds,
                                            int totalItemAmount){
        // 특정 도서 대상
        if(targetBookIds != null && !targetBookIds.isEmpty()
        && (targetCategoryIds == null || targetCategoryIds.isEmpty())){
            return calcContextList.stream()
                    .filter(ctx -> targetBookIds.contains(ctx.getBookId()))
                    .mapToInt(OrderItemCalcContext::getItemTotalPrice)
                    .sum();
        // 특정 카테고리 도서 대상
        }else if((targetBookIds == null || targetBookIds.isEmpty())
                && targetCategoryIds != null && !targetCategoryIds.isEmpty()){
            return calcContextList.stream()
                    .filter(ctx -> targetCategoryIds.contains(ctx.getCategoryId()))
                    .mapToInt(OrderItemCalcContext::getItemTotalPrice)
                    .sum();
        // 금액 대상
        }else if((targetBookIds == null || targetBookIds.isEmpty())
                && targetCategoryIds == null || targetCategoryIds.isEmpty()){
            return totalItemAmount;

        // 특정 도서, 카테고리 대상
        }else{
            return calcContextList.stream()
                    .filter(ctx ->
                            (targetBookIds.contains(ctx.getBookId()) || targetCategoryIds.contains(ctx.getCategoryId())))
                    .mapToInt(OrderItemCalcContext::getItemTotalPrice)
                    .sum();
        }
    }

    /**
     * 포인트 할인 검증 및 생성 메서드
     * @param userId
     * @param point
     * @return 포인트로 할인된 금액
     */
    private int createPointDiscount(Long userId, Integer currentAmount, Integer point){
        if(point == null){
            return 0;
        }
        CurrentPointResponseDto currentPointResponseDto = userServiceClient.getUserPoint(userId);
        int currentPoint = currentPointResponseDto.getCurrentPoint();

        if(currentPoint < point){
            throw new ExceedUserPointException("사용 가능한 포인트를 초과했습니다 (현재 포인트 : %d, 요청 포인트 : %d)".formatted(
                    currentPoint,
                    point
            ));
        }

        if (currentAmount - point < 0) {
            throw new ExceedUserPointException("사용 포인트가 결제 금액보다 더 많습니다 (결제 금액 : %d, 요청 포인트 : %d)".formatted(
                    currentAmount,
                    point
            ));
        }
        return point;
    }

    /**
     * 요청받은 원하는 배송일 검증 및 생성 메서드
     * @param wantDeliveryDate
     * @return 원하는 배송일
     */
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
    public OrderDetailResponseDto getOrderDetail(Long userId, String orderNumber) {
        log.info("일반 사용자 주문 상세 정보 조회 로직 실행 (유저 아이디 : {}, 주문번호 : {})", userId, orderNumber);

        Order order = validateOrderExistence(userId, orderNumber);

        List<Long> bookIds = order.getOrderItems().stream()
                .map(OrderItem::getBookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);

        OrderDetailResponseDto orderDetailResponseDto = orderViewAssembler.toOrderDetailView(order, bookOrderResponseList);

        PaymentResponse paymentResponse = getPaymentInfo(orderNumber);

        orderDetailResponseDto.setPaymentResponse(paymentResponse);

        return orderDetailResponseDto;
    }

    // 일반 사용자 주문 취소
    @Transactional
    @Override
    public OrderCancelResponseDto cancelOrder(Long userId, String orderNumber, OrderCancelRequestDto req) {
        log.info("일반 사용자 주문 취소 로직 실행 (유저 아이디 : {}, 주문번호 : {})", userId, orderNumber);

        Order order = validateOrderExistence(userId, orderNumber);

        Payment payment = paymentRepository.findByOrderNumber(orderNumber).orElseThrow();

        CommonCancelResponse cancelResponse = cancelPaymentExternally(payment, req.cancelReason());

        List<PaymentCancelResponse> paymentCancelResponseList = savePaymentCancel(cancelResponse);

        order.setOrderStatus(OrderStatus.CANCELED);
        Order saved = orderRepository.save(order);

        return new OrderCancelResponseDto(
                saved.getOrderNumber(),
                saved.getOrderStatus().getDescription(),
                paymentCancelResponseList
        );
    }

    // 결제 취소에 필요한 로직
    public Order validateOrderExistence(Long userId, String orderNumber){
        return orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Not Found Order : " + orderNumber));
    }

    public CommonCancelResponse cancelPaymentExternally(Payment payment, String reason){
        CommonCancelRequest cancelRequest = new CommonCancelRequest(payment.getPaymentKey(), null, reason);
        String provider = payment.getPaymentProvider().name();
        PaymentStrategy paymentStrategy = paymentStrategyFactory.getStrategy(provider);
        return paymentStrategy.cancelPayment(cancelRequest, payment.getOrderNumber());
    }

    public List<PaymentCancelResponse> savePaymentCancel(CommonCancelResponse cancelResponse){
        PaymentCancelCreateRequest createRequest = cancelResponse.toPaymentCancelCreateRequest();
        return paymentService.createPaymentCancel(createRequest);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentInfo(String orderNumber){
        return paymentService.getPayment(new PaymentRequest(orderNumber));
    }

    @Override
    public OrderPrepareResponseDto prepareGuestOrder(String guestId, OrderPrepareRequestDto req) {
        log.info("주문 전 데이터 정보 가져오기 로직 실행 (비회원 유저 : {})", guestId);

        List<Long> bookIds = req.bookItems().stream()
                .map(BookInfoDto::bookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);

        String orderNumber = orderNumberProvider.provideOrderNumber();

        return new OrderPrepareResponseDto(
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

    @Transactional(readOnly = true)
    @Override
    public Boolean existsOrderByUserIdAndOrderNumber(Long userId, String orderNumber) {
        return orderRepository.existsByOrderNumberAndUserId(orderNumber, userId);
    }

    @Override
    public Integer findTotalAmoundByOrderNumber(String orderNumber) {
        return orderRepository.findTotalAmount(orderNumber).orElseThrow(
                () -> new OrderNotFoundException("주문을 찾을 수 없습니다 : " + orderNumber));
    }


}
