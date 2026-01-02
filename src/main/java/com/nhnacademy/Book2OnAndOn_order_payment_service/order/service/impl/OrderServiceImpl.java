package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.impl;

import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.CouponPolicyDiscountType;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.CouponTargetResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.CurrentPointResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.MemberCouponResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.OrderCouponCheckRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.dto.UserAddressResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.assembler.OrderViewAssembler;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.CouponServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.client.UserServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderCreateResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderDetailResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderPrepareResponseDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderSimpleDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderStatusUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.OrderVerificationResult;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestOrderCreateRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.BookInfoDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemCalcContext;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemStatusUpdateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryAddress;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.DeliveryPolicy;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.GuestOrder;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.Order;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItem;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper.WrappingPaper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.DeliveryPolicyNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.ExceedUserPointException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.InvalidDeliveryDateException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotCancellableException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.provider.OrderNumberProvider;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderResourceManager;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.OrderTransactionService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.service.WrappingPaperService;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.response.PaymentResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final DeliveryPolicyRepository deliveryPolicyRepository;
    private final GuestOrderRepository guestOrderRepository;

    private final WrappingPaperService wrappingPaperService;
    private final PaymentService paymentService;
    private final OrderTransactionService orderTransactionService;

    private final BookServiceClient bookServiceClient;
    private final UserServiceClient userServiceClient;
    private final CouponServiceClient couponServiceClient;

    private final OrderNumberProvider orderNumberProvider;
    private final PasswordEncoder passwordEncoder;

    private final OrderViewAssembler orderViewAssembler;
    private final OrderResourceManager resourceManager;

    /**
     * 책 클라이언트를 통해 책 정보를 가져오는 공용 메서드입니다.
     * @param bookIds 도서 아이디 List
     * @return 책 정보 반환 List
     */
    private List<BookOrderResponse> fetchBookInfo(List<Long> bookIds){
        List<BookOrderResponse> bookOrderResponseList = bookServiceClient.getBooksForOrder(bookIds);
        log.info("도서 정보 클라이언트 호출 성공, 조회 도서 수 : {}", bookOrderResponseList.size());

        return bookOrderResponseList;
    }

    /**
     * 책 클라이언트를 통해 책 정보를 가져오는 공용 메서드입니다.
     * @param userId 유저 아이디
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
     * @param userId 유저 아이디
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
    public OrderPrepareResponseDto prepareOrder(Long userId, String guestId, OrderPrepareRequestDto req) {
        log.info("주문 전 데이터 정보 가져오기 로직 실행 (회원 아이디 : {}, 비회원 아이디 : {})", userId, guestId);

        // 책 id
        List<Long> bookIds = req.bookItems().stream()
                .map(BookInfoDto::bookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);

        Map<Long, Integer> quantities = req.bookItems().stream()
                .collect(Collectors.toMap(
                        BookInfoDto::bookId,
                        BookInfoDto::quantity
                ));

        for (BookOrderResponse bookOrderResponse : bookOrderResponseList) {
            Integer quantity = quantities.get(bookOrderResponse.getBookId());
            if(quantity != null){
                bookOrderResponse.setQuantity(quantity);
            }
        }

        if(userId == null){
            return OrderPrepareResponseDto.forGuest(bookOrderResponseList);
        }

        List<UserAddressResponseDto> userAddressResponseDtoList = fetchUserAddressInfo(userId);

        // 사용 가능한 쿠폰을 받기 위한 RequestDto 생성
        OrderCouponCheckRequestDto orderCouponCheckRequestDto = createOrderCouponCheckRequest(bookOrderResponseList);
        List<MemberCouponResponseDto> userCouponResponseDtoList = fetchUsableMemberCouponInfo(userId, orderCouponCheckRequestDto);



        CurrentPointResponseDto userCurrentPoint = fetchPointInfo(userId);

        // 회원 주문은 배송지, 쿠폰 및 포인트 여부도 가져옴
        return OrderPrepareResponseDto.forMember(
                bookOrderResponseList,
                userAddressResponseDtoList,
                userCouponResponseDtoList,
                userCurrentPoint
        );
    }

    @Override
    public OrderCreateResponseDto createPreOrder(Long userId, String guestId, OrderCreateRequestDto req) {
        log.info("임시 주문 데이터 생성 및 검증 로직 실행 (회원 아이디 : {}, 비회원 아이디 : {})", userId, guestId);

        OrderVerificationResult result = verifyOrder(userId, guestId, req);
        OrderCreateResponseDto orderCreateResponseDto = null;
        try{
            orderCreateResponseDto = orderTransactionService.createPendingOrder(userId, result);
        } catch (Exception e) {
            log.error("주문 DB 생성 중 오류 : {}", e.getMessage());
            throw new OrderVerificationException("주문 DB 생성 중 오류 " + e.getMessage());
        }

        try {
            // 선점 메서드
            resourceManager.prepareResources(userId, req, result, orderCreateResponseDto.getOrderId());
            return orderCreateResponseDto;
        } catch (Exception e){
            log.error("알 수 없는 오류 발생! 복구 트랜잭션 실행 : {}", e.getMessage());
            // 복구 메서드
            resourceManager.releaseResources(result.orderNumber(), req.getMemberCouponId(), userId, result.pointDiscount(), orderCreateResponseDto.getOrderId());
            throw new OrderVerificationException("주문 내부 오류 발생 " + e.getMessage());
        }
    }

    // 임시 주문 생성을 위한 헬퍼 메서드
    private OrderVerificationResult verifyOrder(Long userId, String guestId, OrderCreateRequestDto req) {
        log.info("주문 데이터 생성 및 검증 로직 실행 (회원 아이디 : {}, 비회원 아이디 : {})", userId, guestId);

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

        if(totalAmount < 100){
            log.error("최소 결제 금액 100원 이상 결제해야합니다 (현재 주문 금액 : {}원)", totalAmount);
        }

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
                pointDiscount, // 포인트 사용량
                wantDeliveryDate,
                orderItemList,
                deliveryAddress
        );
    }


    /**
     * 사용 가능한 쿠폰을 받기위해 쿠폰 서비스에 요청하는 Dto 생성 로직
     * @param resp 도서 정보 리스트
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
     * 임시 주문 생성을 위한 OrderItem 엔티티 리스트 만드는 로직
     * @param bookOrderResponseList 도서 정보 리스트
     * @param orderItemRequestDtoList 도서 항목 요청 리스트
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
     * @param deliveryAddressRequestDto 배송지 요청
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
     * @param bookOrderResponseList 도서 정보 리스트
     * @return 주문명
     */
    private String createOrderTitle(List<BookOrderResponse> bookOrderResponseList){
        StringBuilder sb = new StringBuilder(bookOrderResponseList.getFirst().getTitle());

        sb.setLength(Math.min(sb.length(), 90));

        int size = bookOrderResponseList.size() - 1;

        boolean truncated =
                bookOrderResponseList.getFirst().getTitle().length() > 90;

        if (truncated) {
            sb.append("...");
        }

        if(size >= 2){
            sb.append(" 외 ").append(size).append("건");
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
     * @param orderItemList 주문 항목 리스트
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
     * @param couponId 쿠폰 아이디
     * @param orderItemList 주문 항목 리스트
     * @param bookOrderResponseList 도서 정보 리스트
     * @param totalItemAmount 총 항목 금액
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
            log.error("최소 주문 금액 {}원 이상부터 할인 적용이 가능합니다 (현재 주문 금액 : {}원)", couponTargetResponseDto.minPrice(), discountBaseAmount);
            throw new OrderVerificationException("최소 주문 금액 " + couponTargetResponseDto.minPrice() + "원 이상부터 할인 쿠폰 적용이 가능합니다.");
        }

        if(discountBaseAmount - couponTargetResponseDto.discountValue() < 100){
            log.error("최소 결제 금액 100원 이상 결제해야합니다 (현재 주문 금액 : {}원, 쿠폰 할인 금액 : {}원)", discountBaseAmount, couponTargetResponseDto.discountValue());
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
     * @param userId 유저 아이디
     * @param currentAmount 결제 금액
     * @param point 요청 포인트
     * @return 포인트로 할인된 금액
     */
    private int createPointDiscount(Long userId, Integer currentAmount, Integer point){
        if(point == null || userId == null){
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

        if (currentAmount - point < 100) {
            throw new ExceedUserPointException("최소 결제 금액 100원 이상 결제해야합니다 (결제 금액 : %d, 요청 포인트 : %d)".formatted(
                    currentAmount,
                    point
            ));
        }
        return point;
    }

    /**
     * 요청받은 원하는 배송일 검증 및 생성 메서드
     * @param wantDeliveryDate 원하는 배송일
     * @return 원하는 배송일
     */
    private LocalDate createWantDeliveryDate(LocalDate wantDeliveryDate){
        LocalDate today = LocalDate.now();
        LocalDate minDate = today.plusDays(1);
        LocalDate maxDate = today.plusWeeks(1).plusDays(1);

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
        return orderRepository.findAllByUserId(userId, pageable, OrderStatus.PENDING);
    }

    @Override
    public OrderDetailResponseDto getOrderDetail(Long userId, String orderNumber) {
        log.info("일반 사용자 주문 상세 정보 조회 로직 실행 (유저 아이디 : {}, 주문번호 : {})", userId, orderNumber);

        Order order = orderTransactionService.validateOrderExistence(userId, orderNumber);

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
    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNumber) {
        log.info("일반 사용자 주문 취소 로직 실행 (유저 아이디 : {}, 주문번호 : {})", userId, orderNumber);

        // 주문 검증
        Order order = orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Not Found Order : " + orderNumber));

        // 주문 상태에 따른 오류 던지기
        if(!order.getOrderStatus().isCancellable()){
            log.warn("주문 취소를 할 수 없는 상태입니다 (현재 상태  : {})", order.getOrderStatus().name());
            throw new OrderNotCancellableException("주문 취소를 할 수 없는 상태입니다 : " + order.getOrderStatus().name());
        }

        // 결제 취소 호출 (사용자 주문 취소)
        paymentService.cancelPayment(new PaymentCancelRequest(order.getOrderNumber(), "사용자 주문 취소", null));
        PaymentResponse payment = null;
        try {
            payment = paymentService.getPayment(new PaymentRequest(orderNumber));
        } catch (Exception ignore) {
            // NotFoundPaymentException 등: 결제 전 취소로 처리
        }
        if (payment != null /* && payment.status == APPROVED 같은 조건 */) {
            paymentService.cancelPayment(new PaymentCancelRequest(orderNumber, "사용자 주문 취소", null));
        }

        // 포인트 롤백(이벤트 발생)
//        resourceManager.rollbackPoint(order.getOrderId(), order.getUserId(), order.getPointDiscount());
        // 결제 후 차감된 포인트가 있을 때만 환원
        if (order.getPointDiscount() != null && order.getPointDiscount() > 0) {
            resourceManager.rollbackPoint(order.getOrderId(), order.getUserId(), order.getPointDiscount());
        }

        // 상태 변경
        orderTransactionService.changeStatusOrder(order, false);
    }

    private PaymentResponse getPaymentInfo(String orderNumber){
        return paymentService.getPayment(new PaymentRequest(orderNumber));
    }


    /*
        =================== [비회원 전용 서비스 로직] ====================
     */
    @Override
    public OrderPrepareResponseDto prepareGuestOrder(String guestId, OrderPrepareRequestDto req) {
        log.info("주문 전 데이터 정보 가져오기 로직 실행 (비회원 유저 : {})", guestId);

        List<Long> bookIds = req.bookItems().stream()
                .map(BookInfoDto::bookId)
                .toList();

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);

        return new OrderPrepareResponseDto(
                bookOrderResponseList,
                null,
                null,
                null
        );
    }

    @Override
    @Transactional
    public OrderCreateResponseDto createGuestPreOrder(String guestId, GuestOrderCreateRequestDto req) {
        log.info("비회원 임시 주문 데이터 생성 및 검증 로직 실행 (비회원 아이디 : {})", guestId);

        OrderCreateRequestDto orderCreateRequestDto = new OrderCreateRequestDto(
                req.getOrderItems(),
                req.getDeliveryAddress(),
                req.getDeliveryPolicyId(),
                req.getWantDeliveryDate(),
                null,
                null
        );

        OrderCreateResponseDto orderCreateResponseDto = createPreOrder(null, guestId, orderCreateRequestDto);

        Order order = orderTransactionService.getOrderEntity(orderCreateResponseDto.getOrderNumber());

        GuestOrder guestOrder = GuestOrder.builder()
                .order(order)
                .guestName(req.getGuestName())
                .guestPhoneNumber(req.getGuestPhoneNumber())
                .guestPassword(passwordEncoder.encode(req.getGuestPassword()))
                .build();

        try{
            guestOrderRepository.save(guestOrder);
        } catch (Exception e) {
            log.error("비회원 주문 DB 생성 중 오류 : {}", e.getMessage());
            throw new OrderVerificationException("비회원 주문 DB 생성 중 오류 " + e.getMessage());
        }

        return orderCreateResponseDto;
    }

    // 관리자 전용

    @Override
    @Transactional
    public Page<OrderSimpleDto> getOrderListWithAdmin(Pageable pageable) {
        return orderRepository.findAllByAdmin(pageable);
    }

    @Override
    public OrderDetailResponseDto getOrderDetailWithAdmin(String orderNumber) {

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException("Not Found Order : " + orderNumber));

        List<Long> bookIds = order.getOrderItems().stream()
                        .map(OrderItem::getBookId)
                        .toList();

        List<BookOrderResponse> bookOrderResponseList = fetchBookInfo(bookIds);

        OrderDetailResponseDto orderDetailResponseDto = orderViewAssembler.toOrderDetailView(order, bookOrderResponseList);

        PaymentResponse paymentResponse = getPaymentInfo(orderNumber);

        orderDetailResponseDto.setPaymentResponse(paymentResponse);

        return orderDetailResponseDto;
    }

    @Override
    @Transactional
    public void setOrderStatus(String orderNumber, OrderStatusUpdateDto req) {
        log.info("주문 상태 변경 로직 실행 (주문번호 : {})", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException("Not Found Order : " + orderNumber));

        order.setOrderStatus(req.getOrderStatus());
    }

    @Override
    @Transactional
    public void setOrderItemStatus(String orderNumber, OrderItemStatusUpdateDto req) {
        log.info("주문 항목 상태 변경 로직 실행 (주문번호 : {})", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException("Not Found Order : " + orderNumber));

        for (OrderItem orderItem : order.getOrderItems()) {
            Long id = orderItem.getOrderItemId();

            if(Objects.equals(req.orderItemId(), id)){
                orderItem.setOrderItemStatus(req.orderItemStatus());
            }
        }
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
}
