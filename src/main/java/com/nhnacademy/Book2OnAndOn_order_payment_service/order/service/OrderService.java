package com.nhnacademy.Book2OnAndOn_order_payment_service.order.service;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.guest.GuestOrderCreateDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemDetailDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.orderitem.OrderItemRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.NotFoundOrderException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNotFoundException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.delivery.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.wrappingpaper.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.*;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order.DeliveryAddressRequestDto;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryAddressRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.delivery.DeliveryPolicyRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.GuestOrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderItemRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.order.OrderRepository;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.repository.wrapping.WrappingPaperRepository;

// 외부 모듈 관련 FeignClient 및 외부 DTO Import
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.BookServiceClient;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.StockDecreaseRequest;

import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.api.Cancel;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentCancelCreateRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdatePaymentStatusRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.dto.request.PaymentUpdateRefundAmountRequest;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.Payment;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.domain.entity.PaymentStatus;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.service.PaymentService;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import feign.FeignException;
import java.util.function.Function;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** 주문 생성, 상태 변경, 조회 등 주문 관련 핵심 비즈니스 로직 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final GuestOrderRepository guestOrderRepository;
    private final WrappingPaperRepository wrappingPaperRepository;
    private final DeliveryPolicyRepository deliveryPolicyRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookServiceClient bookServiceClient;
    private final PaymentService paymentService;

    // ======================================================================
    // 1. 주문 생성 API
    // ======================================================================

    /**
     * [회원] 주문을 생성하고 결제 전 필요한 정보 준비
     */
    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDto request) {
        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 반드시 존재해야 합니다.");
        }
        OrderPriceCalculationDto priceDto = calculateOrderPrices(request);
        // 1. 재고 차감 요청 DTO 생성
        List<StockDecreaseRequest> stockRequests = request.getOrderItems().stream()
                .map(item -> StockDecreaseRequest.builder()
                        .bookId(item.getBookId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
        try {
            // 2. FeignClient 호출: 재고 차감 요청 (409 Conflict 발생 가능)
            bookServiceClient.decreaseStock(stockRequests);

            // 3. 성공 시 주문 생성 계속 진행
            Order order = buildAndSaveOrder(request, priceDto);
            saveOrderItems(request.getOrderItems(), order, priceDto.getBookMap());
            saveDeliveryAddress(request.getDeliveryAddress(), order);

            return convertToOrderResponseDto(order);

        } catch (FeignException e) {
            // 409 Conflict 예외 처리 (재고 부족)
            if (e.status() == 409) {
                throw new IllegalStateException("재고 부족으로 주문에 실패했습니다.");
            }
            // 기타 통신 오류 처리
            throw new RuntimeException("도서 서비스와의 통신 오류가 발생했습니다.", e);
        }

    }

    /**
     * [비회원] 주문을 생성하고 결제 전 필요한 정보 준비
     */
    @Transactional
    public OrderResponseDto createGuestOrder(GuestOrderCreateDto request) {
        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 반드시 존재해야 합니다.");
        }
        
        // GuestOrderCreateDto를 OrderCreateRequestDto로 변환하여 로직 재사용
        OrderCreateRequestDto orderRequest = convertToOrderRequest(request);
        OrderPriceCalculationDto priceDto = calculateOrderPrices(orderRequest);

        List<StockDecreaseRequest> stockRequests = orderRequest.getOrderItems().stream()
                .map(item -> new StockDecreaseRequest(item.getBookId(), item.getQuantity()))
                .collect(Collectors.toList());

        try {
            // 2. FeignClient 호출: 재고 차감 요청 (409 Conflict 발생 가능)
            bookServiceClient.decreaseStock(stockRequests);

            // 3. 성공 시 주문 생성 계속 진행
            Order order = buildAndSaveOrder(orderRequest, priceDto);
            saveOrderItems(orderRequest.getOrderItems(), order, priceDto.getBookMap());
            saveDeliveryAddress(orderRequest.getDeliveryAddress(), order);

            // 비회원 정보 저장
            saveGuestOrderInfo(request, order);

            return convertToOrderResponseDto(order);

        } catch (FeignException e) {
            if (e.status() == 409) {
                throw new IllegalStateException("재고 부족으로 주문에 실패했습니다.");
            }
            throw new RuntimeException("도서 서비스 통신 오류가 발생했습니다.", e);
        }
    }


    // ======================================================================
    // 2. 주문 조회 및 목록 API
    // ======================================================================

    /**
     * [회원/관리자 공통] 주문 상세 정보를 조회
     */
    @Transactional(readOnly = true)
    public OrderResponseDto findOrderDetails(Long orderId, Long userId) {
        // N+1 문제 해결을 위해 Fetch Join 쿼리 사용
        Order order = orderRepository.findOrderWithDetails(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // 권한 검증 로직
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 주문에 대한 접근 권한이 없습니다.");
        }
        
        return convertToOrderResponseDto(order);
    }

    /**
     * [비회원] 주문 상세 조회 (비밀번호 검증 포함)
     */
    @Transactional(readOnly = true)
    public OrderResponseDto findGuestOrderDetails(Long orderId, String password) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        Optional<GuestOrder> guestOrderOptional = guestOrderRepository.findByOrder_OrderId(orderId);

        if (guestOrderOptional.isEmpty()) {
            throw new OrderNotFoundException("비회원 주문 정보를 찾을 수 없습니다.");
        }

        GuestOrder guestOrder = guestOrderOptional.get();
        
        // 비밀번호 검증
        if (!passwordEncoder.matches(password, guestOrder.getGuestPassword())) {
            throw new AccessDeniedException("비밀번호가 일치하지 않습니다.");
        }
        
        return convertToOrderResponseDto(order);
    }

    /**
     * [회원] 본인의 주문 목록을 조회 (Pagination 구현)
     */
    @Transactional(readOnly = true)
    public Page<OrderSimpleDto> findOrderList(Long userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        return orderPage.map(this::convertToOrderSimpleDto);
    }

    /**
     * [관리자] 모든 주문 목록을 조회(Pagination 구현)
     */
    @Transactional(readOnly = true)
    public Page<OrderSimpleDto> findAllOrderList(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return  orderPage.map(this::convertToOrderSimpleDto);
    }

    // ======================================================================
    // 3. 주문 취소 및 관리자 API
    // ======================================================================

    /**
     * [회원] 주문 취소
     */
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, Long userId, OrderCancelRequestDto request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인의 주문만 취소할 수 있습니다.");
        }
        
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("배송 준비 중이거나 결제 대기 중인 주문만 취소 가능합니다.");
        }
        
        order.setOrderStatus(OrderStatus.CANCELED);

        // 재고 복구 (increaseStock) 로직 호출
        List<StockDecreaseRequest> stockRestoreRequests = orderItemRepository.findByOrder_OrderId(orderId).stream()
                .map(item -> new StockDecreaseRequest(item.getBookId(), item.getOrderItemQuantity()))
                .collect(Collectors.toList());
        try{
            bookServiceClient.increaseStock(stockRestoreRequests);
        } catch (FeignException e) {
            throw new RuntimeException("도서 서비스 재고 복구 오류가 발생했습니다.", e);
        }
        //  취소 정보를 PaymentInfo 등에 기록하는 로직 추가
        processPaymentCancellation(order, request);

        return convertToOrderResponseDto(order);
    }

    /**
     * [비회원] 주문을 취소 (비밀번호 검증 포함)
     */
    @Transactional
    public OrderResponseDto cancelGuestOrder(Long orderId, String password) {
        // 1. 비밀번호 검증
        findGuestOrderDetails(orderId, password); 

        // 2. 취소 로직 (취소 사유 DTO가 없으므로 임시로 null 사용)
        return cancelOrder(orderId, null, new OrderCancelRequestDto(null, null, null)); 
    }
    
    /**
     * [관리자] 주문 상태를 변경
     */
    @Transactional
    public OrderResponseDto updateOrderStatusByAdmin(Long orderId, OrderStatusUpdateDto request) {
        // 1. TINYINT 코드를 OrderStatus Enum으로 변환
        OrderStatus newStatus = OrderStatus.fromCode(request.getStatusCode());
        
        // 2. 상태 변경 (유효성 검사는 updateOrderStatus에서 처리)
        updateOrderStatusInternal(orderId, newStatus);
        
        // 3. 변경된 엔티티 조회 및 DTO 변환
        Order order = orderRepository.findById(orderId).get();
        return convertToOrderResponseDto(order);
    }

    // ======================================================================
    // 4. 헬퍼 메서드 및 DTO 변환
    // ======================================================================

    // 주문 상태 변경
    // 이 메서드를 통해 테스트 코드가 접근
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        // 내부 헬퍼 메서드를 호출하여 로직 실행
        updateOrderStatusInternal(orderId, newStatus);
    }

    @Transactional
    protected void updateOrderStatusInternal(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELED || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("이미 최종 처리된 주문은 상태를 변경할 수 없습니다.");
        }

        if (order.getOrderStatus() == OrderStatus.SHIPPING && newStatus == OrderStatus.PENDING) {
            throw new IllegalArgumentException("배송 중 상태에서 대기 상태로 되돌릴 수 없습니다.");
        }
        order.setOrderStatus(newStatus);
    }

    /**
     * [DTO 변환] Order 엔티티를 OrderResponseDto로 변환
     */
    private OrderResponseDto convertToOrderResponseDto(Order order) {
        // OrderItem, DeliveryAddress 등 연관 관계 DTO 변환 및 합치는 로직
        // 1. OrderItemDetailDto 리스트 생성 (외부 데이터 조회 필요)
        List<Long> bookIds = orderItemRepository.findByOrder_OrderId(order.getOrderId()).stream()
                .map(OrderItem::getBookId) // OrderItem에서 bookId 추출
                .collect(Collectors.toList());
        // BookClient를 통해 외부에서 BookTitle 등 정보 조회
        List<BookOrderResponse> bookInfos = bookServiceClient.getBooksForOrder(bookIds);
        Map<Long, BookOrderResponse> bookMap = bookInfos.stream()
                .collect(Collectors.toMap(BookOrderResponse::getBookId, Function.identity()));

        // OrderItemDetailDto 리스트는 OrderItem 엔티티를 조회하여 만들어야 함
        List<OrderItemDetailDto> itemDetails = order.getOrderItems().stream()
                .map(item -> convertToOrderItemDetailDto(item, bookMap.get(item.getBookId()))) // Map에서 Book 정보 전달
                .collect(Collectors.toList());

        // DeliveryAddressRequestDto는 DeliveryAddress 엔티티를 조회하여 만들어야 함
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findByOrder_OrderId(order.getOrderId()).orElse(null);
        DeliveryAddressRequestDto addressDto = convertToDeliveryAddressRequestDto(deliveryAddress);

        return new OrderResponseDto(
            order.getOrderId(),
            order.getOrderNumber(),
            order.getOrderStatus(),
            order.getOrderDatetime(),
            order.getTotalAmount(),
            order.getTotalDiscountAmount(),
            order.getTotalItemAmount(),
            order.getDeliveryFee(),
            order.getWrappingFee(),
            order.getCouponDiscount(),
            order.getPointDiscount(),
            order.getWantDeliveryDate(),
            itemDetails,
            addressDto
        );
    }

    /**
     * [DTO 변환] Order 엔티티를 OrderSimpleDto로 변환합니다.
     */
    private OrderSimpleDto convertToOrderSimpleDto(Order order) {
        // 1. OrderItem 목록이 있는지 확인
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            // 주문 항목이 없으면 기본값으로 반환
            return new OrderSimpleDto(
                    order.getOrderId(),
                    order.getOrderNumber(),
                    order.getOrderStatus(),
                    order.getOrderDatetime(),
                    order.getTotalAmount(),
                    "상품 없음"
            );
        }

        // 2. 대표 상품 ID 추출 (첫 번째 OrderItem의 bookId 사용)
        Long representativeBookId = order.getOrderItems().stream()
                .findFirst()
                .map(OrderItem::getBookId)
                .orElse(null);

        String representativeTitle = "제목 없음";

        if (representativeBookId != null) {
            try {
                // 3. FeignClient 호출: BookService에서 해당 도서의 정보만 조회
                // List<Long>을 받는 API를 사용하므로 Collections.singletonList() 사용
                List<BookOrderResponse> bookInfos = bookServiceClient.getBooksForOrder(
                        Collections.singletonList(representativeBookId)
                );

                // 4. 제목 추출
                if (!bookInfos.isEmpty()) {
                    representativeTitle = bookInfos.get(0).getTitle();
                }
            } catch (FeignException e) {
                // 외부 통신 실패 시에도 주문 목록은 볼 수 있도록 예외를 던지지 않고 로그만 남김
                System.err.println("WARN: Best seller list generation failed due to Feign communication error: " + e.getMessage());
                representativeTitle = "조회 오류";
            }
        }

        // 5. OrderSimpleDto 객체 생성 및 반환
        return new OrderSimpleDto(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getOrderDateTime(),
                order.getTotalAmount(),
                representativeTitle // 조회된 제목 사용
        );
    }

    @Transactional
    protected void processPaymentCancellation(Order order, OrderCancelRequestDto request) {

        String orderNumber = order.getOrderNumber();
        Integer cancelAmount = order.getTotalAmount(); // 전액 취소 가정

        // 1. Payment 엔티티 조회 (OrderNumber로 조회)
        // Payment 엔티티를 찾지 못하면 여기서 NotFoundPaymentException 발생
        // Payment payment = paymentRepository.findByOrderNumber(orderNumber);

        // 2. PaymentCancelCreateRequest DTO 구성 (취소 내역 기록)
        // Payment 엔티티에서 paymentKey를 가져와야 합니다. (findByOrderNumber 호출 후)
        String paymentKey = "TOSS_PK_MOCKED"; // TODO: 임시 PaymentKey 교체해야함

        PaymentCancelCreateRequest cancelRequest = new PaymentCancelCreateRequest(
                paymentKey,
                PaymentStatus.CANCEL.name(), // 최종 결제 상태
                List.of(
                        new Cancel(
                                cancelAmount, // 취소할 금액
                                request.getCancelReason(),
                                LocalDateTime.now()
                        )
                )
        );
        paymentService.createPaymentCancel(cancelRequest);

        // 3. 환불 금액 업데이트 요청
        PaymentUpdateRefundAmountRequest refundRequest = new PaymentUpdateRefundAmountRequest(
                orderNumber,
                paymentKey
        );
        paymentService.updateRefundAmount(refundRequest);

        // 4. 결제 상태 업데이트 요청
        PaymentUpdatePaymentStatusRequest statusRequest = new PaymentUpdatePaymentStatusRequest(
                orderNumber,
                PaymentStatus.CANCEL.name() // 최종 상태를 CANCEL로 설정
        );
        paymentService.updatePaymentStatus(statusRequest);
    }
    
    // ----------------------------------------------------------------------
    // DTO 변환 헬퍼 (OrderResponseDto를 위한 하위 객체 생성)
    // ----------------------------------------------------------------------
    
    /**
     * OrderItem 엔티티를 OrderItemDetailDto로 변환
     */
    private OrderItemDetailDto convertToOrderItemDetailDto(OrderItem item, BookOrderResponse bookInfo) {
        String bookTitle = bookInfo != null ? bookInfo.getTitle() : "제목 없음";
        String wrappingPaperName = item.getWrappingPaper() != null ? item.getWrappingPaper().getWrappingPaperName() : null;

        return new OrderItemDetailDto(
            item.getOrderItemId(),
            bookTitle,
            item.getOrderItemQuantity(),
            item.getUnitPrice(),
            item.getOrderItemQuantity() * item.getUnitPrice(), // totalPrice 계산
            wrappingPaperName,
            item.getOrderItemStatus().getDescription() // Enum to String

        );
    }
    
    /**
     * DeliveryAddress 엔티티를 DeliveryAddressRequestDto로 변환
     */
    private DeliveryAddressRequestDto convertToDeliveryAddressRequestDto(DeliveryAddress info) {
        if (info == null) return null;
        return new DeliveryAddressRequestDto(
            info.getDeliveryAddress(),
            info.getDeliveryAddressDetail(),
            info.getDeliveryMessage(),
            info.getRecipient(),
            info.getRecipientPhonenumber()
        );
    }

    /**
     * [DTO 변환] GuestOrderCreateDto를 OrderCreateRequestDto로 변환
     */
    private OrderCreateRequestDto convertToOrderRequest(GuestOrderCreateDto guestRequest) {
        return new OrderCreateRequestDto(
            null, // 비회원이므로 userId는 null
            guestRequest.getOrderItems(), 
            guestRequest.getDeliveryAddress(),
            guestRequest.getCouponDiscountAmount(),
            guestRequest.getPointDiscountAmount()
        );
    }


    /**
     * Order 엔티티를 생성 -> 초기 상태로 저장
     */
    private Order buildAndSaveOrder(OrderCreateRequestDto request, OrderPriceCalculationDto priceDto) {
        String orderNumber = "B2" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        int totalDiscount = request.getCouponDiscountAmount() + request.getPointDiscountAmount();
        
        int totalAmount = priceDto.getTotalItemPrice()
                + priceDto.getTotalWrappingFee()
                + priceDto.getDeliveryFee()
                - totalDiscount;

        Order order = Order.builder()
            .orderNumber(orderNumber)
            .orderDatetime(LocalDateTime.now())
            .orderStatus(OrderStatus.PENDING)
            .userId(request.getUserId())
            .couponDiscount(request.getCouponDiscountAmount())
            .pointDiscount(request.getPointDiscountAmount())
            .totalDiscountAmount(totalDiscount)
            .totalItemAmount(priceDto.getTotalItemPrice())
            .wrappingFee(priceDto.getTotalWrappingFee())
            .deliveryFee(priceDto.getDeliveryFee())
            .totalAmount(totalAmount)
            .build();

        return orderRepository.save(order);
    }

    /**
     * OrderItem 엔티티 리스트를 생성하여 DB에 저장-> 재고 차감
     */
    private void saveOrderItems(List<OrderItemRequestDto> itemRequests, Order order, Map<Long, BookOrderResponse> bookMap) {
        for (OrderItemRequestDto itemRequest : itemRequests) {
            
            // 1. 포장지 조회
            WrappingPaper wrappingPaper = itemRequest.getWrappingPaperId() != null ? 
                wrappingPaperRepository.findById(itemRequest.getWrappingPaperId()).orElse(null) : 
                null;

            BookOrderResponse book = bookMap.get(itemRequest.getBookId());

            // 2. OrderItem 엔티티 생성
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .bookId(itemRequest.getBookId())
                    .orderItemQuantity(itemRequest.getQuantity())
                    .unitPrice(book.getPriceSales()) // 임시 단가
                    .isWrapped(itemRequest.isWrapped())
                    .orderItemStatus(OrderItemStatus.PREPARING)
                    .wrappingPaper(wrappingPaper)
                    .build();

            orderItemRepository.save(orderItem);
        }
    }

    private void saveDeliveryAddress(DeliveryAddressRequestDto addressRequest, Order order) {

        DeliveryAddress addressInfo = DeliveryAddress.builder()
                .order(order)
                .deliveryAddress(addressRequest.getDeliveryAddress())
                .deliveryAddressDetail(addressRequest.getDeliveryAddressDetail())
                .deliveryMessage(addressRequest.getDeliveryMessage())
                .recipient(addressRequest.getRecipient())
                .recipientPhonenumber(addressRequest.getRecipientPhonenumber())
                .build();

        deliveryAddressRepository.save(addressInfo);
    }

    private void saveGuestOrderInfo(GuestOrderCreateDto guestRequest, Order order) {
        String encryptedPassword = passwordEncoder.encode(guestRequest.getGuestPassword());

        GuestOrder guestOrder = GuestOrder.builder()
            .order(order)
            .guestName(guestRequest.getGuestName())
            .guestPhonenumber(guestRequest.getGuestPhonenumber())
            .guestPassword(encryptedPassword)
            .build();

        guestOrderRepository.save(guestOrder);
    }

    /** OrderItem, WrappingPaper 정보를 조회하여 모든 금액 계산 */
    private OrderPriceCalculationDto calculateOrderPrices(OrderCreateRequestDto request) {
        int totalItemPrice = 0;
        int totalWrappingFee = 0;

        // 1. 주문 항목에서 Book ID 리스트 추출
        List<Long> bookIds = request.getOrderItems().stream()
                .map(OrderItemRequestDto::getBookId)
                .collect(Collectors.toList());

        // 2. FeignClient 호출: Book-Service에서 도서 정보 목록 조회
        List<BookOrderResponse> bookInfos = bookServiceClient.getBooksForOrder(bookIds);
        Map<Long, BookOrderResponse> bookMap = bookInfos.stream()
                .collect(Collectors.toMap(BookOrderResponse::getBookId, Function.identity()));

        // 3. 가격 계산 및 포장비 조회
        for (OrderItemRequestDto itemRequest : request.getOrderItems()) {
            BookOrderResponse book = bookMap.get(itemRequest.getBookId());
            if (book == null) {
                throw new OrderVerificationException("유효하지 않은 상품 ID가 포함되었습니다: " + itemRequest.getBookId());
            }

            // 실제 판매가(priceSales)를 사용
            totalItemPrice += (book.getPriceSales() * itemRequest.getQuantity());

            if (itemRequest.isWrapped()) {
                // 실제 WrappingPaperRepository에서 가격 정보 조회
                WrappingPaper wrappingPaper = wrappingPaperRepository.findById(itemRequest.getWrappingPaperId())
                        .orElseThrow(() -> new IllegalArgumentException("포장지 가격 조회를 위해 포장지 ID를 찾을 수 없습니다."));

                totalWrappingFee += wrappingPaper.getWrappingPaperPrice();
            }
        }

        // 2. 배송비 정책 적용
        DeliveryPolicy defaultPolicy = deliveryPolicyRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("기본 배송 정책을 찾을 수 없습니다."));

        int deliveryFee = calculateDeliveryCost(totalItemPrice, defaultPolicy);

        return new OrderPriceCalculationDto(totalItemPrice, totalWrappingFee, deliveryFee, bookMap);
    }

    /**총 상품 가격과 정책 기반 배송비 계산*/
    private int calculateDeliveryCost(int totalItemPrice, DeliveryPolicy policy) {
        if (totalItemPrice >= policy.getFreeDeliveryThreshold()) {
            return 0;
        }
        return policy.getDeliveryFee();
    }

    public boolean existsOrder(String orderNumber, Long userId){
        return orderRepository.existsByOrderNumberAndUserId(orderNumber, userId);
    }

    public Integer getTotalAmount(String orderNumber){
        return orderRepository.findTotalAmount(orderNumber).orElseThrow(()->new NotFoundOrderException("Not Found Order : " + orderNumber));
    }

    // 이 DTO는 Service 내부에서만 사용
    @Getter
    @AllArgsConstructor
    private static class OrderPriceCalculationDto {
        private final int totalItemPrice;
        private final int totalWrappingFee;
        private final int deliveryFee;
        private final Map<Long, BookOrderResponse> bookMap;
    }
}